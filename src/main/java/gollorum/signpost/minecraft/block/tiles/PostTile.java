package gollorum.signpost.minecraft.block.tiles;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.types.*;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.items.Wrench;
import gollorum.signpost.minecraft.utils.SideUtils;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.utils.WaystoneContainer;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.Ray;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.BlockPosSerializer;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.ItemStackSerializer;
import gollorum.signpost.utils.serialization.StringSerializer;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fmllegacy.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PostTile extends BlockEntity implements WithOwner.OfSignpost, WithOwner.OfWaystone, WaystoneContainer {

    public static final String REGISTRY_NAME = "post";

    public static final BlockEntityType<PostTile> type = BlockEntityType.Builder.of(
        (pos, state) -> new PostTile(
            PostBlock.ModelType.Oak,
            ItemStack.EMPTY,
            pos, state
        ),
        PostBlock.ALL.toArray(new Block[0])
    ).build(null);

    private final Map<UUID, BlockPartInstance> parts = new ConcurrentHashMap<>();
    public static final Set<BlockPartMetadata<?>> partsMetadata = new ConcurrentSet<>();
    static {
        partsMetadata.add(PostBlockPart.METADATA);
        partsMetadata.add(SmallWideSignBlockPart.METADATA);
        partsMetadata.add(SmallShortSignBlockPart.METADATA);
        partsMetadata.add(LargeSignBlockPart.METADATA);
        partsMetadata.add(WaystoneBlockPart.METADATA);
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

    public final PostBlock.ModelType modelType;
    private ItemStack drop;
    private Optional<PlayerHandle> owner = Optional.empty();

    public PostTile(PostBlock.ModelType modelType, ItemStack drop, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.modelType = modelType;
        this.drop = drop;
    }

    public UUID addPart(BlockPartInstance part, ItemStack cost, PlayerHandle player){ return addPart(UUID.randomUUID(), part, cost, player); }

    public UUID addPart(UUID identifier, BlockPartInstance part, ItemStack cost, PlayerHandle player){
        parts.put(identifier, part);
        if(hasLevel() && !getLevel().isClientSide()) sendToTracing(() -> new PartAddedEvent.Packet(
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
        if(getLevel() != null && !getLevel().isClientSide())
            sendToTracing(() -> new PartRemovedEvent.Packet(new TilePartInfo(this, id), false));
        oldPart.blockPart.removeFrom(this);
        setChanged();
        return oldPart;
    }

    @Override
    public void setRemoved() {
        for (BlockPartInstance part: parts.values()) part.blockPart.removeFrom(this);
        super.setRemoved();
    }

    public Collection<BlockPartInstance> getParts(){ return parts.values(); }

    public VoxelShape getBounds(){
        return parts.values().stream().map(t -> t
            .blockPart.getIntersection()
            .getBounds()
            .offset(t.offset)
            .asMinecraftBB()
        ).map(Shapes::create)
        .reduce((b1, b2) -> Shapes.join(b1, b2, BooleanOp.OR)).orElse(Shapes.empty());
    }

    @Override
    public AABB getRenderBoundingBox() {
        VoxelShape shape = getBounds();
        return shape.isEmpty()
            ? new AABB(getBlockPos())
            : shape.bounds().move(getBlockPos());
    }

    public Optional<TraceResult> trace(Player player){
        Vec3 head = player.position();
        head = head.add(0, player.getEyeHeight(), 0);
        if (player.isCrouching())
            head = head.subtract(0, 0.08, 0);
        Vec3 look = player.getLookAngle();
        Ray ray = new Ray(Vector3.fromVec3d(head).subtract(Vector3.fromBlockPos(getBlockPos())), Vector3.fromVec3d(look));

        Optional<Tuple<UUID, Float>> closestTrace = Optional.empty();
        for(Map.Entry<UUID, BlockPartInstance> t : parts.entrySet()){
            Optional<Float> now = t.getValue().blockPart.intersectWith(ray, t.getValue().offset);
            if(now.isPresent() && (!closestTrace.isPresent() || closestTrace.get().getB() > now.get()))
                closestTrace = Optional.of(new Tuple<>(t.getKey(), now.get()));
        }

        return closestTrace.map(trace -> new TraceResult(parts.get(trace.getA()), trace.getA(), ray.atDistance(trace.getB()), ray));
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        super.save(compound);
        writeSelf(compound);
        return compound;
    }

    private void writeSelf(CompoundTag compound){
        for(Map.Entry<BlockPartMetadata, List<Map.Entry<UUID, BlockPartInstance>>> entry: parts.entrySet().stream()
            .collect(Collectors.groupingBy(p -> p.getValue().blockPart.getMeta())).entrySet()
        ){
            List<Map.Entry<UUID, BlockPartInstance>> instances = entry.getValue();
            compound.putInt(entry.getKey().identifier, instances.size());
            for(int i=0; i<instances.size(); i++){
                Map.Entry<UUID, BlockPartInstance> e = instances.get(i);
                BlockPartInstance instance = e.getValue();
                CompoundTag subComp = instance.blockPart.write();
                subComp.put("Offset", Vector3.Serializer.write(instance.offset));
                compound.put(entry.getKey().identifier + "_" + i, subComp);
                subComp.putUUID("PartId", e.getKey());
            }
        }
        compound.put("Drop", ItemStackSerializer.Instance.write(drop));
        compound.put("Owner", PlayerHandle.Serializer.optional().write(owner));
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        readSelf(compound);
    }

    private void readSelf(CompoundTag compound){
        parts.clear();
        for(BlockPartMetadata<?> meta : partsMetadata){
            for(int i = 0; i < compound.getInt(meta.identifier); i++){
                CompoundTag comp = compound.getCompound(meta.identifier + "_" + i);
                addPart(
                    comp.getUUID("PartId"),
                    new BlockPartInstance(
                        meta.read(comp),
                        Vector3.Serializer.read(comp.getCompound("Offset"))
                    ),
                    ItemStack.EMPTY,
                    PlayerHandle.Invalid
                );
            }
        }
        drop = ItemStackSerializer.Instance.read(compound.getCompound("Drop"));
        owner = PlayerHandle.Serializer.optional().read(compound.getCompound("Owner"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag ret = super.getUpdateTag();
        writeSelf(ret);
        return ret;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        CompoundTag ret = new CompoundTag();
        writeSelf(ret);
        return new ClientboundBlockEntityDataPacket(getBlockPos(), 1, ret);
    }

    @Override
    public void handleUpdateTag(CompoundTag compound) {
        super.handleUpdateTag(compound);
        readSelf(compound);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        readSelf(pkt.getTag());
    }

    public void notifyMutation(UUID part, CompoundTag data, String partMetaIdentifier) {
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
        return getParts().stream().filter(p -> p.blockPart instanceof WaystoneBlockPart).findFirst()
            .flatMap(p -> ((WaystoneBlockPart)p.blockPart).getWaystoneOwner());
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

        public TilePartInfo(BlockEntity tile, UUID identifier) {
            this.dimensionKey = tile.getLevel().dimension().location();
            this.pos = tile.getBlockPos();
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
            public CompoundTag write(TilePartInfo tilePartInfo, CompoundTag compound) {
                compound.putString("Dimension", tilePartInfo.dimensionKey.toString());
                compound.put("Pos", BlockPosSerializer.INSTANCE.write(tilePartInfo.pos, compound));
                compound.putUUID("Id", tilePartInfo.identifier);
                return compound;
            }

            @Override
            public boolean isContainedIn(CompoundTag compound) {
                return compound.contains("Dimension")
                    && compound.contains("Pos")
                    && compound.contains("Id");
            }

            @Override
            public TilePartInfo read(CompoundTag compound) {
                return new TilePartInfo(
                    new ResourceLocation(compound.getString("Dimension")),
                    BlockPosSerializer.INSTANCE.read(compound.getCompound("Pos")),
                    compound.getUUID("Id")
                );
            }

            @Override
            public Class<TilePartInfo> getTargetClass() {
                return TilePartInfo.class;
            }

            @Override
            public void write(TilePartInfo tilePartInfo, FriendlyByteBuf buffer) {
                buffer.writeResourceLocation(tilePartInfo.dimensionKey);
                BlockPosSerializer.INSTANCE.write(tilePartInfo.pos, buffer);
                buffer.writeUUID(tilePartInfo.identifier);
            }

            @Override
            public TilePartInfo read(FriendlyByteBuf buffer) {
                return new TilePartInfo(
                    buffer.readResourceLocation(),
                    BlockPosSerializer.INSTANCE.read(buffer),
                    buffer.readUUID()
                );
            }
        };

    }

    public static class PartAddedEvent implements PacketHandler.Event<PartAddedEvent.Packet> {

        public static class Packet {
            public final TilePartInfo info;
            public final String partMetaIdentifier;
            public final CompoundTag partData;
            public final Vector3 offset;
            public final ItemStack cost;
            public final PlayerHandle player;

            public Packet(
                TilePartInfo info,
                CompoundTag partData,
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
        public void encode(Packet message, FriendlyByteBuf buffer) {
            TilePartInfo.Serializer.write(message.info, buffer);
            StringSerializer.instance.write(message.partData.getAsString(), buffer);
            StringSerializer.instance.write(message.partMetaIdentifier, buffer);
            Vector3.Serializer.write(message.offset, buffer);
            buffer.writeItem(message.cost);
            PlayerHandle.Serializer.write(message.player, buffer);
        }

        @Override
        public Packet decode(FriendlyByteBuf buffer) {
            try {
                return new Packet(
                    TilePartInfo.Serializer.read(buffer),
                    TagParser.parseTag(StringSerializer.instance.read(buffer)),
                    StringSerializer.instance.read(buffer),
                    Vector3.Serializer.read(buffer),
                    buffer.readItem(),
                    PlayerHandle.Serializer.read(buffer)
                );
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
                throw new RuntimeException("An exception occurred in PostTile Packet NBT decoding");
            }
        }

        @Override
        public void handle(Packet message, NetworkEvent.Context context) {
            boolean isClientSide = context.getDirection().getReceptionSide().isClient();
            TileEntityUtils.findTileEntity(
                message.info.dimensionKey,
                isClientSide,
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
                           (!isClientSide ||
                                (SideUtils.getClientPlayer().map(player -> player.getUUID().equals(message.player.id)).orElse(false)))) {
                        SideUtils.makePlayerPayIfEditor(isClientSide, context.getSender(), message.player, message.cost);
                    }
                    tile.setChanged();
                } else {
                    Signpost.LOGGER.warn("Could not find meta for part " + message.partMetaIdentifier);
                }
            });
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
        public void encode(Packet message, FriendlyByteBuf buffer) {
            TilePartInfo.Serializer.write(message.info, buffer);
            buffer.writeBoolean(message.shouldDropItem);
        }

        @Override
        public Packet decode(FriendlyByteBuf buffer) {
            return new Packet(TilePartInfo.Serializer.read(buffer), buffer.readBoolean());
        }

        @Override
        public void handle(Packet message, NetworkEvent.Context context) {
            boolean isClient = context.getDirection().getReceptionSide().isClient();
            TileEntityUtils.findWorld(message.info.dimensionKey, isClient).ifPresent(level ->
                TileEntityUtils.delayUntilTileEntityExistsAt(
                    new WorldLocation(message.info.pos, level),
                    PostTile.class,
                    tile -> {
                        BlockPartInstance oldPart = tile.removePart(message.info.identifier);
                        if(oldPart != null && !tile.getLevel().isClientSide() && !context.getSender().isCreative() && message.shouldDropItem){
                            for(ItemStack item : (Collection<ItemStack>) oldPart.blockPart.getDrops(tile)) {
                                if(!context.getSender().getInventory().add(item))
                                    if(tile.getLevel() instanceof ServerLevel) {
                                        ServerLevel serverWorld = (ServerLevel) tile.getLevel();
                                        BlockPos pos = message.info.pos;
                                        ItemEntity itementity = new ItemEntity(
                                            serverWorld,
                                            pos.getX() + serverWorld.getRandom().nextFloat() * 0.5 + 0.25,
                                            pos.getY() + serverWorld.getRandom().nextFloat() * 0.5 + 0.25,
                                            pos.getZ() + serverWorld.getRandom().nextFloat() * 0.5 + 0.25,
                                            item
                                        );
                                        itementity.setDefaultPickUpDelay();
                                        serverWorld.addFreshEntity(itementity);
                                    }
                            }
                        }
                    },
                    100,
                    isClient,
                    Optional.of(() -> Signpost.LOGGER.error("Failed to process PartRemovedEvent, tile was not present"))
                )
            );
        }
    }

    public static class PartMutatedEvent implements PacketHandler.Event<PartMutatedEvent.Packet> {

        public static class Packet {
            public final TilePartInfo info;
            public final CompoundTag data;
            public final String partMetaIdentifier;
            public final Optional<Vector3> offset;

            public Packet(TilePartInfo info, CompoundTag data, String partMetaIdentifier) {
                this(info, data, partMetaIdentifier, Optional.empty());
            }

            public Packet(TilePartInfo info, CompoundTag data, String partMetaIdentifier, Vector3 offset) {
                this(info, data, partMetaIdentifier, Optional.of(offset));
            }

            public Packet(TilePartInfo info, CompoundTag data, String partMetaIdentifier, Optional<Vector3> offset) {
                this.info = info;
                this.data = data;
                this.partMetaIdentifier = partMetaIdentifier;
                this.offset = offset;
            }

        }
        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, FriendlyByteBuf buffer) {
            TilePartInfo.Serializer.write(message.info, buffer);
            StringSerializer.instance.write(message.data.toString(), buffer);
            StringSerializer.instance.write(message.partMetaIdentifier, buffer);
            Vector3.Serializer.optional().write(message.offset, buffer);
        }

        @Override
        public Packet decode(FriendlyByteBuf buffer) {
            try {
                return new Packet(
                    TilePartInfo.Serializer.read(buffer),
                    TagParser.parseTag(StringSerializer.instance.read(buffer)),
                    StringSerializer.instance.read(buffer),
                    Vector3.Serializer.optional().read(buffer)
                );
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void handle(Packet message, NetworkEvent.Context context) {
            boolean isServer = context.getDirection().getReceptionSide().isServer();
            TileEntityUtils.findWorld(message.info.dimensionKey, !isServer).ifPresent(level ->
                TileEntityUtils.delayUntilTileEntityExistsAt(
                    new WorldLocation(message.info.pos, level),
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
                            tile.setChanged();
                            if(isServer)
                                tile.sendToTracing(() -> message);
                        }
                        else Signpost.LOGGER.error("Tried to mutate a post part that wasn't present: " + message.info.identifier);
                    },
                    100,
                    !isServer,
                    Optional.of(() -> Signpost.LOGGER.error("Failed to process PartMutatedEvent, tile was not present"))
                ));
        }
    }
}
