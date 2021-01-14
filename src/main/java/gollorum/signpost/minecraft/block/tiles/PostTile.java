package gollorum.signpost.minecraft.block.tiles;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.types.*;
import gollorum.signpost.minecraft.Wrench;
import gollorum.signpost.minecraft.utils.SideUtils;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.BlockPart;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.utils.TileEntityUtils;
import gollorum.signpost.utils.math.geometry.Ray;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.BlockPosSerializer;
import gollorum.signpost.utils.serialization.BufferSerializable;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.block.BlockState;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.network.PacketBuffer;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PostTile extends TileEntity {

    public static final String REGISTRY_NAME = "post";

    public static final TileEntityType<PostTile> type = TileEntityType.Builder.create(() -> new PostTile(gollorum.signpost.minecraft.block.Post.ModelType.Oak), gollorum.signpost.minecraft.block.Post.ALL).build(null);

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
        public final BlockPart part;
        public final UUID id;
        public final Vector3 hitPos;
        public final Ray ray;
        public TraceResult(BlockPart part, UUID id, Vector3 hitPos, Ray ray) {
            this.part = part;
            this.id = id;
            this.hitPos = hitPos;
            this.ray = ray;
        }
    }

    public final gollorum.signpost.minecraft.block.Post.ModelType modelType;

    public PostTile(gollorum.signpost.minecraft.block.Post.ModelType modelType) {
        super(type);
        this.modelType = modelType;
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
        if(getWorld() != null && !getWorld().isRemote)
            sendToTracing(() -> new PartRemovedEvent.Packet(new TilePartInfo(this, id), false));
        oldPart.blockPart.removeFrom(this);
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

        return closestTrace.map(trace -> new TraceResult(parts.get(trace.getA()).blockPart, trace.getA(), ray.atDistance(trace.getB()), ray));
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
                Vector3.SERIALIZER.writeTo(instance.offset, subComp, "Offset");
                compound.put(entry.getKey().identifier + "_" + i, subComp);
                subComp.putUniqueId("PartId", e.getKey());
            }
        }
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
                        Vector3.SERIALIZER.read(comp, "Offset")
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
    }

    @Override
    public CompoundNBT getUpdateTag() {
        CompoundNBT ret = super.getUpdateTag();
        writeSelf(ret);
        return ret;
    }

    @Override
    public void handleUpdateTag(BlockState blockState, CompoundNBT compound) {
        super.handleUpdateTag(blockState, compound);
        readSelf(compound);
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
        return parts.values().stream().flatMap(p -> (Stream<ItemStack>) p.blockPart.getDrops(this).stream())
            .collect(Collectors.toList());
    }

    public static boolean isAngleTool(Item item) {
        return item instanceof Wrench;
    }

    public static boolean isEditTool(Item item) {
        return item instanceof AxeItem;
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

        public static final Serializer SERIALIZER = new Serializer();

        public static final class Serializer implements BufferSerializable<TilePartInfo> {

            @Override
            public void writeTo(TilePartInfo tilePartInfo, PacketBuffer buffer) {
                buffer.writeResourceLocation(tilePartInfo.dimensionKey);
                BlockPosSerializer.INSTANCE.writeTo(tilePartInfo.pos, buffer);
                buffer.writeUniqueId(tilePartInfo.identifier);
            }

            @Override
            public TilePartInfo readFrom(PacketBuffer buffer) {
                return new TilePartInfo(
                    buffer.readResourceLocation(),
                    BlockPosSerializer.INSTANCE.readFrom(buffer),
                    buffer.readUniqueId()
                );
            }
        }

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
            TilePartInfo.SERIALIZER.writeTo(message.info, buffer);
            buffer.writeString(message.partData.getString());
            buffer.writeString(message.partMetaIdentifier);
            Vector3.SERIALIZER.writeTo(message.offset, buffer);
            buffer.writeItemStack(message.cost);
            PlayerHandle.SERIALIZER.writeTo(message.player, buffer);
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            try {
                return new Packet(
                    TilePartInfo.SERIALIZER.readFrom(buffer),
                    JsonToNBT.getTagFromJson(buffer.readString(32767)),
                    buffer.readString(32767),
                    Vector3.SERIALIZER.readFrom(buffer),
                    buffer.readItemStack(),
                    PlayerHandle.SERIALIZER.readFrom(buffer)
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
            TilePartInfo.SERIALIZER.writeTo(message.info, buffer);
            buffer.writeBoolean(message.shouldDropItem);
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            return new Packet(TilePartInfo.SERIALIZER.readFrom(buffer), buffer.readBoolean());
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() ->
                TileEntityUtils.findTileEntity(
                    message.info.dimensionKey,
                    context.get().getDirection().getReceptionSide().isClient(),
                    message.info.pos,
                    PostTile.class
                ).ifPresent(tile -> {
                    BlockPartInstance oldPart = tile.removePart(message.info.identifier);
                    if(oldPart != null && !tile.world.isRemote && !context.get().getSender().isCreative() && message.shouldDropItem){
                        for(ItemStack item : (Collection<ItemStack>) oldPart.blockPart.getDrops(tile)) {
                            if(!context.get().getSender().inventory.addItemStackToInventory(item))
                                if(tile.world instanceof ServerWorld) {
                                    ServerWorld world = (ServerWorld) tile.world;
                                    BlockPos pos = message.info.pos;
                                        ItemEntity itementity = new ItemEntity(
                                            world,
                                            pos.getX() + world.rand.nextFloat() * 0.5 + 0.25,
                                            pos.getY() + world.rand.nextFloat() * 0.5 + 0.25,
                                            pos.getZ() + world.rand.nextFloat() * 0.5 + 0.25,
                                            item
                                        );
                                        itementity.setDefaultPickupDelay();
                                        world.addEntity(itementity);
                                }
                        }
                    }
                })
            );
            context.get().setPacketHandled(true);
        }
    }

    public static class PartMutatedEvent implements PacketHandler.Event<PartMutatedEvent.Packet> {

        public static class Packet {
            public final TilePartInfo info;
            public final CompoundNBT data;
            public final String partMetaIdentifier;

            public Packet(TilePartInfo info, CompoundNBT data, String partMetaIdentifier) {
                this.info = info;
                this.data = data;
                this.partMetaIdentifier = partMetaIdentifier;
            }
        }
        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, PacketBuffer buffer) {
            TilePartInfo.SERIALIZER.writeTo(message.info, buffer);
            buffer.writeString(message.data.toString());
            buffer.writeString(message.partMetaIdentifier);
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            try {
                return new Packet(
                    TilePartInfo.SERIALIZER.readFrom(buffer),
                    JsonToNBT.getTagFromJson(buffer.readString(32767)),
                    buffer.readString(32767));
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() ->
                TileEntityUtils.findTileEntity(
                    message.info.dimensionKey,
                    context.get().getDirection().getReceptionSide().isClient(),
                    message.info.pos,
                    PostTile.class
                ).ifPresent(tile -> {
                    Optional<BlockPartInstance> part = tile.getPart(message.info.identifier);
                    if(part.isPresent()) {
                        if(part.get().blockPart.getMeta().identifier.equals(message.partMetaIdentifier)) {
                            part.get().blockPart.readMutationUpdate(message.data, tile);
                        } else {
                            Optional<BlockPartMetadata<?>> meta = partsMetadata.stream().filter(m -> m.identifier.equals(message.partMetaIdentifier)).findFirst();
                            if (meta.isPresent()) {
                                tile.parts.remove(message.info.identifier);
                                tile.parts.put(message.info.identifier,
                                    new BlockPartInstance(meta.get().read(message.data), part.get().offset));
                                tile.markDirty();
                            } else {
                                Signpost.LOGGER.warn("Could not find meta for part " + message.partMetaIdentifier);
                            }
                        }
                        if(context.get().getDirection().getReceptionSide().isServer())
                            tile.sendToTracing(() -> message);
                    }
                    else Signpost.LOGGER.error("Tried to mutate a post part that wasn't present: " + message.info.identifier);
                }));
            context.get().setPacketHandled(true);
        }
    }
}
