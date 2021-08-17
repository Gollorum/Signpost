package gollorum.signpost.minecraft.block.tiles;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.types.*;
import gollorum.signpost.minecraft.items.Wrench;
import gollorum.signpost.minecraft.utils.SideUtils;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.utils.WaystoneContainer;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.Ray;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.BlockPosSerializer;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.ItemStackSerializer;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PostTile extends TileEntity implements WithOwner.OfSignpost, WithOwner.OfWaystone, WaystoneContainer {

    public static final String REGISTRY_NAME = "post";

    public static final TileEntityType<PostTile> type = TileEntityType.Builder.create(
        () -> new PostTile(
            gollorum.signpost.minecraft.block.Post.ModelType.Oak,
            ItemStack.EMPTY
        ),
        gollorum.signpost.minecraft.block.Post.ALL.toArray(new Block[0])
    ).build(null);

    private final Map<UUID, BlockPartInstance> parts = new ConcurrentHashMap<>();
    public static final Set<BlockPartMetadata<?>> partsMetadata = new ConcurrentSet<>();
    static {
        partsMetadata.add(Post.METADATA);
        partsMetadata.add(SmallWideSign.METADATA);
        partsMetadata.add(SmallShortSign.METADATA);
        partsMetadata.add(LargeSign.METADATA);
        partsMetadata.add(Waystone.METADATA);
    }

	public static class TraceResult {
        public final BlockPartInstance part;
        public final UUID id;
        public final Vector3 hitPos;
        public final Ray ray;
        public TraceResult(BlockPartInstance part, UUID id, Vector3 hitPos, Ray ray) {
            this.part = part;
            this.id = id;
            this.hitPos = hitPos;
            this.ray = ray;
        }
    }

    public final gollorum.signpost.minecraft.block.Post.ModelType modelType;
    private ItemStack drop;
    private Optional<PlayerHandle> owner = Optional.empty();

    public PostTile(gollorum.signpost.minecraft.block.Post.ModelType modelType, ItemStack drop) {
        super(type);
        this.modelType = modelType;
        this.drop = drop;
    }

    public UUID addPart(BlockPartInstance part, ItemStack cost, PlayerHandle player){ return addPart(UUID.randomUUID(), part, cost, player); }

    public UUID addPart(UUID identifier, BlockPartInstance part, ItemStack cost, PlayerHandle player){
        parts.put(identifier, part);
        if(hasWorld() && !world.isRemote) sendToTracing(() -> new PartAddedEvent.Packet(
            new TilePartInfo(this, identifier),
            part.blockPart.write(),
            part.blockPart.getMeta().identifier,
            part.offset,
            cost, player
        ));
        return identifier;
    }

    public BlockPartInstance removePart(UUID id) {
        BlockPartInstance oldPart = parts.remove(id);
        if(oldPart == null) {
            Signpost.LOGGER.error("Failed to remove post block part with id " + id);
            return oldPart;
        }
        if(getWorld() != null && !getWorld().isRemote)
            sendToTracing(() -> new PartRemovedEvent.Packet(new TilePartInfo(this, id), false));
        oldPart.blockPart.removeFrom(this);
        markDirty();
        return oldPart;
    }

    @Override
    public void remove() {
        for (BlockPartInstance part: parts.values()) part.blockPart.removeFrom(this);
        super.remove();
    }

    public Collection<BlockPartInstance> getParts(){ return parts.values(); }

    public VoxelShape getBounds(){
        return parts.values().stream().map(t -> t
            .blockPart.getIntersection()
            .getBounds()
            .offset(t.offset)
            .asMinecraftBB()
        ).map(VoxelShapes::create)
        .reduce((b1, b2) -> VoxelShapes.combine(b1, b2, IBooleanFunction.OR)).orElse(VoxelShapes.empty());
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        VoxelShape shape = getBounds();
        return shape.isEmpty()
            ? new AxisAlignedBB(pos)
            : shape.getBoundingBox().offset(pos);
    }

    public Optional<TraceResult> trace(PlayerEntity player){
        Vector3d head = player.getPositionVec();
        head = head.add(0, player.getEyeHeight(), 0);
        if (player.isCrouching())
            head = head.subtract(0, 0.08, 0);
        Vector3d look = player.getLookVec();
        Ray ray = new Ray(Vector3.fromVec3d(head).subtract(Vector3.fromBlockPos(pos)), Vector3.fromVec3d(look));

        Optional<Tuple<UUID, Float>> closestTrace = Optional.empty();
        for(Map.Entry<UUID, BlockPartInstance> t : parts.entrySet()){
            Optional<Float> now = t.getValue().blockPart.intersectWith(ray, t.getValue().offset);
            if(now.isPresent() && (!closestTrace.isPresent() || closestTrace.get().getB() > now.get()))
                closestTrace = Optional.of(new Tuple<>(t.getKey(), now.get()));
        }

        return closestTrace.map(trace -> new TraceResult(parts.get(trace.getA()), trace.getA(), ray.atDistance(trace.getB()), ray));
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        super.write(compound);
        writeSelf(compound);
        return compound;
    }

    private void writeSelf(CompoundNBT compound){
        for(Map.Entry<BlockPartMetadata, List<Map.Entry<UUID, BlockPartInstance>>> entry: parts.entrySet().stream()
            .collect(Collectors.groupingBy(p -> p.getValue().blockPart.getMeta())).entrySet()
        ){
            List<Map.Entry<UUID, BlockPartInstance>> instances = entry.getValue();
            compound.putInt(entry.getKey().identifier, instances.size());
            for(int i=0; i<instances.size(); i++){
                Map.Entry<UUID, BlockPartInstance> e = instances.get(i);
                BlockPartInstance instance = e.getValue();
                CompoundNBT subComp = instance.blockPart.write();
                subComp.put("Offset", Vector3.Serializer.write(instance.offset));
                compound.put(entry.getKey().identifier + "_" + i, subComp);
                subComp.putUniqueId("PartId", e.getKey());
            }
        }
        compound.put("Drop", ItemStackSerializer.Instance.write(drop));
        compound.put("Owner", PlayerHandle.Serializer.optional().write(owner));
    }

    @Override
    public void read(BlockState blockState, CompoundNBT compound) {
        super.read(blockState, compound);
        readSelf(compound);
    }

    private void readSelf(CompoundNBT compound){
        parts.clear();
        for(BlockPartMetadata<?> meta : partsMetadata){
            for(int i = 0; i < compound.getInt(meta.identifier); i++){
                CompoundNBT comp = compound.getCompound(meta.identifier + "_" + i);
                addPart(
                    comp.getUniqueId("PartId"),
                    new BlockPartInstance(
                        meta.read(comp),
                        Vector3.Serializer.read(comp.getCompound("Offset"))
                    ),
                    ItemStack.EMPTY,
                    PlayerHandle.Invalid
                );
            }
        }
        if(parts.isEmpty()) addPart(
            new BlockPartInstance(new Post(gollorum.signpost.minecraft.block.Post.ModelType.Oak.postTexture), Vector3.ZERO),
            ItemStack.EMPTY,
            PlayerHandle.Invalid
        );
        drop = ItemStackSerializer.Instance.read(compound.getCompound("Drop"));
        owner = PlayerHandle.Serializer.optional().read(compound.getCompound("Owner"));
    }

    @Override
    public CompoundNBT getUpdateTag() {
        CompoundNBT ret = super.getUpdateTag();
        writeSelf(ret);
        return ret;
    }

    @Nullable
    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        CompoundNBT ret = new CompoundNBT();
        writeSelf(ret);
        return new SUpdateTileEntityPacket(getPos(), 1, ret);
    }

    @Override
    public void handleUpdateTag(BlockState blockState, CompoundNBT compound) {
        super.handleUpdateTag(blockState, compound);
        readSelf(compound);
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        super.onDataPacket(net, pkt);
        readSelf(pkt.getNbtCompound());
    }

    public void notifyMutation(UUID part, CompoundNBT data, String partMetaIdentifier) {
        sendToTracing(
            () -> new PostTile.PartMutatedEvent.Packet(
                new PostTile.TilePartInfo(this, part),
                data,
                partMetaIdentifier)
        );
    }

    public <T> void sendToTracing(Supplier<T> t) {
        PacketHandler.sendToTracing(this, t);
    }

    public Collection<ItemStack> getDrops() {
        List<ItemStack> ret = parts.values().stream().flatMap(p -> (Stream<ItemStack>) p.blockPart.getDrops(this).stream())
            .collect(Collectors.toList());
        ret.add(drop);
        return ret;
    }

    public void setSignpostOwner(Optional<PlayerHandle> owner) {
        this.owner = owner;
    }

    public Optional<PlayerHandle> getSignpostOwner() { return owner; }

    public Optional<PlayerHandle> getWaystoneOwner() {
        return getParts().stream().filter(p -> p.blockPart instanceof Waystone).findFirst()
            .flatMap(p -> ((Waystone)p.blockPart).getWaystoneOwner());
    }

    public static boolean isAngleTool(Item item) {
        return item instanceof Wrench;
    }

    public Optional<BlockPartInstance> getPart(UUID id) {
        return parts.containsKey(id) ? Optional.of(parts.get(id)) : Optional.empty();
    }

    public static class TilePartInfo {
        public final ResourceLocation dimensionKey;
        public final BlockPos pos;
        public final UUID identifier;

        public TilePartInfo(TileEntity tile, UUID identifier) {
            this.dimensionKey = tile.getWorld().getDimensionKey().getLocation();
            this.pos = tile.getPos();
            this.identifier = identifier;
        }

        public TilePartInfo(ResourceLocation dimensionKey, BlockPos pos, UUID identifier) {
            this.dimensionKey = dimensionKey;
            this.pos = pos;
            this.identifier = identifier;
        }

        public static final CompoundSerializable<TilePartInfo> Serializer = new SerializerImpl();
        public static final class SerializerImpl implements CompoundSerializable<TilePartInfo> {

            @Override
            public CompoundNBT write(TilePartInfo tilePartInfo, CompoundNBT compound) {
                compound.putString("Dimension", tilePartInfo.dimensionKey.toString());
                compound.put("Pos", BlockPosSerializer.INSTANCE.write(tilePartInfo.pos, compound));
                compound.putUniqueId("Id", tilePartInfo.identifier);
                return compound;
            }

            @Override
            public boolean isContainedIn(CompoundNBT compound) {
                return compound.contains("Dimension")
                    && compound.contains("Pos")
                    && compound.contains("Id");
            }

            @Override
            public TilePartInfo read(CompoundNBT compound) {
                return new TilePartInfo(
                    new ResourceLocation(compound.getString("Dimension")),
                    BlockPosSerializer.INSTANCE.read(compound.getCompound("Pos")),
                    compound.getUniqueId("Id")
                );
            }

            @Override
            public Class<TilePartInfo> getTargetClass() {
                return TilePartInfo.class;
            }

            @Override
            public void write(TilePartInfo tilePartInfo, PacketBuffer buffer) {
                buffer.writeResourceLocation(tilePartInfo.dimensionKey);
                BlockPosSerializer.INSTANCE.write(tilePartInfo.pos, buffer);
                buffer.writeUniqueId(tilePartInfo.identifier);
            }

            @Override
            public TilePartInfo read(PacketBuffer buffer) {
                return new TilePartInfo(
                    buffer.readResourceLocation(),
                    BlockPosSerializer.INSTANCE.read(buffer),
                    buffer.readUniqueId()
                );
            }
        };

    }

    public static class PartAddedEvent implements PacketHandler.Event<PartAddedEvent.Packet> {

        public static class Packet {
            public final TilePartInfo info;
            public final String partMetaIdentifier;
            public final CompoundNBT partData;
            public final Vector3 offset;
            public final ItemStack cost;
            public final PlayerHandle player;

            public Packet(
                TilePartInfo info,
                CompoundNBT partData,
                String partMetaIdentifier,
                Vector3 offset,
                ItemStack cost,
                PlayerHandle player
            ) {
                this.info = info;
                this.partMetaIdentifier = partMetaIdentifier;
                this.partData = partData;
                this.offset = offset;
                this.cost = cost;
                this.player = player;
            }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, PacketBuffer buffer) {
            TilePartInfo.Serializer.write(message.info, buffer);
            buffer.writeString(message.partData.getString());
            buffer.writeString(message.partMetaIdentifier);
            Vector3.Serializer.write(message.offset, buffer);
            buffer.writeItemStack(message.cost);
            PlayerHandle.Serializer.write(message.player, buffer);
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            try {
                return new Packet(
                    TilePartInfo.Serializer.read(buffer),
                    JsonToNBT.getTagFromJson(buffer.readString(32767)),
                    buffer.readString(32767),
                    Vector3.Serializer.read(buffer),
                    buffer.readItemStack(),
                    PlayerHandle.Serializer.read(buffer)
                );
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
                throw new RuntimeException("An exception occurred in PostTile Packet NBT decoding");
            }
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            boolean isRemote = context.getDirection().getReceptionSide().isClient();
            context.enqueueWork(() ->
                TileEntityUtils.findTileEntity(
                    message.info.dimensionKey,
                    isRemote,
                    message.info.pos,
                    PostTile.class
            ).ifPresent(tile -> {
                Optional<BlockPartMetadata<?>> meta = partsMetadata.stream().filter(m -> m.identifier.equals(message.partMetaIdentifier)).findFirst();
                if (meta.isPresent()) {
                    tile.addPart(message.info.identifier,
                        new BlockPartInstance(meta.get().read(message.partData), message.offset),
                        message.cost,
                        message.player
                    );
                    if(message.cost.getCount() > 0 &&
                           (!isRemote ||
                                (SideUtils.getClientPlayer().map(player -> player.getUniqueID().equals(message.player.id)).orElse(false)))) {
                        PlayerEntity player = isRemote ? SideUtils.getClientPlayer().get() : context.getSender();
                        if (player.getUniqueID().equals(message.player.id)) {
                            if (!player.isCreative())
                                player.inventory.func_234564_a_(
                                    i -> i.getItem().equals(message.cost.getItem()),
                                    message.cost.getCount(),
                                    player.container.func_234641_j_()
                                );
                        } else {
                            Signpost.LOGGER.error(
                                "Tried to apply cost but the player was not the expected one (expected {}, got {})",
                                message.player.id,
                                player.getUniqueID()
                            );
                        }
                    }
                    tile.markDirty();
                } else {
                    Signpost.LOGGER.warn("Could not find meta for part " + message.partMetaIdentifier);
                }
            }));
            context.setPacketHandled(true);
        }

    }

    public static class PartRemovedEvent implements PacketHandler.Event<PartRemovedEvent.Packet> {

        public static class Packet {
            public final TilePartInfo info;
            public final boolean shouldDropItem;
            public Packet(TilePartInfo info, boolean shouldDropItem) {
                this.info = info;
                this.shouldDropItem = shouldDropItem;
            }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, PacketBuffer buffer) {
            TilePartInfo.Serializer.write(message.info, buffer);
            buffer.writeBoolean(message.shouldDropItem);
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            return new Packet(TilePartInfo.Serializer.read(buffer), buffer.readBoolean());
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() ->
                TileEntityUtils.findWorld(message.info.dimensionKey, context.get().getDirection().getReceptionSide().isClient()).ifPresent(world ->
                    TileEntityUtils.delayUntilTileEntityExistsAt(
                        new WorldLocation(message.info.pos, world),
                        PostTile.class,
                        tile -> {
                            BlockPartInstance oldPart = tile.removePart(message.info.identifier);
                            if(oldPart != null && !tile.getWorld().isRemote && !context.get().getSender().isCreative() && message.shouldDropItem){
                                for(ItemStack item : (Collection<ItemStack>) oldPart.blockPart.getDrops(tile)) {
                                    if(!context.get().getSender().inventory.addItemStackToInventory(item))
                                        if(tile.world instanceof ServerWorld) {
                                            ServerWorld serverWorld = (ServerWorld) tile.world;
                                            BlockPos pos = message.info.pos;
                                            ItemEntity itementity = new ItemEntity(
                                                serverWorld,
                                                pos.getX() + serverWorld.rand.nextFloat() * 0.5 + 0.25,
                                                pos.getY() + serverWorld.rand.nextFloat() * 0.5 + 0.25,
                                                pos.getZ() + serverWorld.rand.nextFloat() * 0.5 + 0.25,
                                                item
                                            );
                                            itementity.setDefaultPickupDelay();
                                            serverWorld.addEntity(itementity);
                                        }
                                }
                            }
                        },
                        100,
                        Optional.of(() -> Signpost.LOGGER.error("Failed to process PartRemovedEvent, tile was not present"))
                    )
            ));
            context.get().setPacketHandled(true);
        }
    }

    public static class PartMutatedEvent implements PacketHandler.Event<PartMutatedEvent.Packet> {

        public static class Packet {
            public final TilePartInfo info;
            public final CompoundNBT data;
            public final String partMetaIdentifier;
            public final Optional<Vector3> offset;

            public Packet(TilePartInfo info, CompoundNBT data, String partMetaIdentifier) {
                this(info, data, partMetaIdentifier, Optional.empty());
            }

            public Packet(TilePartInfo info, CompoundNBT data, String partMetaIdentifier, Vector3 offset) {
                this(info, data, partMetaIdentifier, Optional.of(offset));
            }

            public Packet(TilePartInfo info, CompoundNBT data, String partMetaIdentifier, Optional<Vector3> offset) {
                this.info = info;
                this.data = data;
                this.partMetaIdentifier = partMetaIdentifier;
                this.offset = offset;
            }

        }
        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, PacketBuffer buffer) {
            TilePartInfo.Serializer.write(message.info, buffer);
            buffer.writeString(message.data.toString());
            buffer.writeString(message.partMetaIdentifier);
            Vector3.Serializer.optional().write(message.offset, buffer);
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            try {
                return new Packet(
                    TilePartInfo.Serializer.read(buffer),
                    JsonToNBT.getTagFromJson(buffer.readString(32767)),
                    buffer.readString(32767),
                    Vector3.Serializer.optional().read(buffer)
                );
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> contextGetter) {
            NetworkEvent.Context context = contextGetter.get();
            boolean isServer = context.getDirection().getReceptionSide().isServer();
            context.enqueueWork(() ->
                TileEntityUtils.findWorld(message.info.dimensionKey, !isServer).ifPresent(world ->
                    TileEntityUtils.delayUntilTileEntityExistsAt(
                        new WorldLocation(message.info.pos, world),
                        PostTile.class,
                        tile -> {
                            Optional<BlockPartInstance> part = tile.getPart(message.info.identifier);
                            if(part.isPresent()) {
                                BlockPartInstance blockPartInstance = part.get();
                                if(blockPartInstance.blockPart.getMeta().identifier.equals(message.partMetaIdentifier)) {
                                    blockPartInstance.blockPart.readMutationUpdate(message.data, tile, isServer ? context.getSender() : null);
                                    if(message.offset.isPresent()) {
                                        tile.parts.remove(message.info.identifier);
                                        tile.parts.put(message.info.identifier, new BlockPartInstance(blockPartInstance.blockPart, message.offset.get()));
                                    }
                                } else {
                                    Optional<BlockPartMetadata<?>> meta = partsMetadata.stream().filter(m -> m.identifier.equals(message.partMetaIdentifier)).findFirst();
                                    if (meta.isPresent()) {
                                        tile.parts.remove(message.info.identifier);
                                        tile.parts.put(message.info.identifier,
                                            new BlockPartInstance(meta.get().read(message.data), message.offset.orElse(blockPartInstance.offset)));
                                    } else {
                                        Signpost.LOGGER.warn("Could not find meta for part " + message.partMetaIdentifier);
                                    }
                                }
                                tile.markDirty();
                                if(isServer)
                                    tile.sendToTracing(() -> message);
                            }
                            else Signpost.LOGGER.error("Tried to mutate a post part that wasn't present: " + message.info.identifier);
                        },
                        100,
                        Optional.of(() -> Signpost.LOGGER.error("Failed to process PartMutatedEvent, tile was not present"))
                    )
                ));
            context.setPacketHandled(true);
        }
    }
}
