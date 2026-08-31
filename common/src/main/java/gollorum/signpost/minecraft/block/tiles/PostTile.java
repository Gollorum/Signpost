package gollorum.signpost.minecraft.block.tiles;

import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.types.*;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.minecraft.data.ModelTypeRegistry;
import gollorum.signpost.minecraft.data.PostData;
import gollorum.signpost.minecraft.data.WaystoneHandleData;
import gollorum.signpost.minecraft.items.Wrench;
import gollorum.signpost.minecraft.utils.SideUtils;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.minecraft.worldgen.VillageSignpost;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.platform.Services;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.*;
import gollorum.signpost.utils.math.geometry.Ray;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.Util;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Tuple;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class PostTile extends BlockEntity implements WithOwner.OfSignpost, WithOwner.OfWaystone, WaystoneContainer {

    public static final String REGISTRY_NAME = "post";

    private static BlockEntityType<PostTile> type = null;
    public static BlockEntityType<PostTile> createType() {
        assert type == null;
        Type<?> type = Util.fetchChoiceType(References.BLOCK_ENTITY, REGISTRY_NAME);
        return PostTile.type = Services.BLOCK_ENTITY_TYPE_FACTORY.create(
            PostTile::new,
            // The legacy blocks have to count as valid too, or the block entities in a pre-2.04 save are
            // rejected before anything gets the chance to migrate them.
            PostBlock.allIncludingLegacy().toArray(PostBlock[]::new),
            type
        );
    }
    public static BlockEntityType<PostTile> getBlockEntityType() {
        assert type != null;
        return type;
    }

    private Map<UUID, BlockPartInstance> parts = new ConcurrentHashMap<>();
    public Map<UUID, BlockPartInstance> parts() {
        return parts;
    }
    public static final Map<String, BlockPartMetadata<?>> partsMetadata = new ConcurrentHashMap<>();
    static {
        partsMetadata.put(PostBlockPart.METADATA.identifier(), PostBlockPart.METADATA);
        partsMetadata.put(SmallWideSignBlockPart.METADATA.identifier(), SmallWideSignBlockPart.METADATA);
        partsMetadata.put(SmallShortSignBlockPart.METADATA.identifier(), SmallShortSignBlockPart.METADATA);
        partsMetadata.put(LargeSignBlockPart.METADATA.identifier(), LargeSignBlockPart.METADATA);
        partsMetadata.put(WaystoneBlockPart.METADATA.identifier(), WaystoneBlockPart.METADATA);
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

    private ResourceKey<PostBlock.ModelType> modelTypeKey = null;
    public ResourceKey<PostBlock.ModelType> modelTypeKey() {
        if (modelTypeKey == null) modelTypeKey = postBlock().defaultModelType();
        return modelTypeKey;
    }

    private PostBlock.ModelType modelType = null;
    public PostBlock.ModelType modelType() {
        if (modelType == null && hasLevel())
            modelType = ModelTypeRegistry.getOrFallbackModelType(
                getLevel().registryAccess(), modelTypeKey(), this::getBlockMaterialType);
        return modelType != null ? modelType : ModelTypeRegistry.fallbackModelType(getBlockMaterialType());
    }

    private Optional<PlayerHandle> owner = Optional.empty();

    private final List<Runnable> toDoOnceLevelIsSet = new ArrayList<>();

    public PostTile(BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public UUID addPart(BlockPartInstance part, ItemStack cost, PlayerHandle player){ return addPart(UUID.randomUUID(), part, cost, player); }
    public UUID addPart(BlockPartInstance part, ItemStack cost, PlayerHandle player, boolean shouldNotify){
        return addPart(UUID.randomUUID(), part, cost, player, shouldNotify);
    }

    public UUID addPart(UUID identifier, BlockPartInstance part, ItemStack cost, PlayerHandle player){
        return addPart(identifier, part, cost, player, true);
    }
    public UUID addPart(UUID identifier, BlockPartInstance part, ItemStack cost, PlayerHandle player, boolean shouldNotify){
        parts.put(identifier, part);
        Runnable toDo = () -> {
            part.blockPart().attachTo(this);
            if (shouldNotify && hasLevel() && !getLevel().isClientSide()) sendToTracing(() -> new PartAddedEvent.Packet(
                new TilePartInfo(this, identifier),
                part,
                cost,
                player
            ));
        };
        if(hasLevel()) {
            toDo.run();
        } else {
            toDoOnceLevelIsSet.add(toDo);
        }
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
        oldPart.blockPart().removeFrom(this);
        setChanged();
        return oldPart;
    }

    public void onDestroy() {
        for (BlockPartInstance part: parts.values()) part.blockPart().removeFrom(this);
    }

    public Collection<BlockPartInstance> getParts(){ return parts.values(); }

    public VoxelShape getBounds(){
        return parts.values().stream().map(t -> t
                .blockPart().getIntersection()
            .getBounds()
            .offset(t.offset())
            .asMinecraftBB()
        ).map(Shapes::create)
        .reduce((b1, b2) -> Shapes.join(b1, b2, BooleanOp.OR)).orElse(Shapes.empty());
    }

    public Optional<TraceResult> trace(Entity player){
        Vec3 head = player.position();
        head = head.add(0, player.getEyeHeight(), 0);
        if (player.isCrouching())
            head = head.subtract(0, 0.08, 0);
        Vec3 look = player.getLookAngle();
        Ray ray = new Ray(Vector3.fromVec3d(head).subtract(Vector3.fromBlockPos(getBlockPos())), Vector3.fromVec3d(look));

        Optional<Tuple<UUID, Float>> closestTrace = Optional.empty();
        for(Map.Entry<UUID, BlockPartInstance> t : parts.entrySet()){
            Optional<Float> now = t.getValue().blockPart().intersectWith(ray, t.getValue().offset());
            if(now.isPresent() && (!closestTrace.isPresent() || closestTrace.get().getB() > now.get()))
                closestTrace = Optional.of(new Tuple<>(t.getKey(), now.get()));
        }

        return closestTrace.map(trace -> new TraceResult(parts.get(trace.getA()), trace.getA(), ray.atDistance(trace.getB()), ray));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        writeSelf(output);
    }

    private void writeSelf(ValueOutput output) {
        output.store(PostData.CODEC, new PostData(Optional.of(modelTypeKey()), parts));
        output.store(Codec.optionalField("Owner", PlayerHandle.DIRECT_CODEC, true), owner);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        readSelf(input);
    }

    /**
     * The block this tile belongs to. Read from the tile's own state rather than from the level, so that it is
     * available while the tile is still being deserialized.
     */
    private PostBlock postBlock() {
        return getBlockState().getBlock() instanceof PostBlock post ? post : PostBlock.MaterialType.Wood.getBlock();
    }

    public PostBlock.MaterialType getBlockMaterialType() {
        return postBlock().materialType;
    }

    private void readSelf(ValueInput input) {
        var data = input.read(PostData.CODEC);
        // Data written before 2.04 names no model type: back then the block id was the model type, which is
        // what the block falls back to. See PostBlock.LegacyVariant.
        modelTypeKey = data.flatMap(PostData::modelType).orElseGet(() -> postBlock().defaultModelType());
        modelType = null;
        parts = data
            .map(d -> new ConcurrentHashMap(d.parts()))
            .orElseGet(ConcurrentHashMap::new);
        owner = input.read(Codec.optionalField("Owner", PlayerHandle.DIRECT_CODEC, true)).flatMap(it -> it);

        Runnable init = () -> {
            // Needs the level, because that is what resolves the model type against the datapack registry.
            if (parts.isEmpty())
                parts.put(UUID.randomUUID(), new BlockPartInstance(new PostBlockPart(modelType().postTexture()), Vector3.ZERO));
            for(BlockPartInstance part : parts.values()) {
                part.blockPart().attachTo(this);
            }
        };
        if(hasLevel()) {
            init.run();
        } else {
            toDoOnceLevelIsSet.add(init);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        PostBlock.ModelType.applyTo(modelTypeKey(), components);
        components.set(PostData.TYPE, new PostData(Optional.of(modelTypeKey()), parts));
        getWaystonePart().ifPresent(waystone -> {
            waystone.getHandle().ifPresent(h -> components.set(WaystoneHandleData.TYPE, new WaystoneHandleData(h)));
            waystone.getName().ifPresent(n -> components.set(DataComponents.CUSTOM_NAME, Component.literal(n)));
        });
    }

    public void readData(PostData data, HolderLookup.Provider registryAccess, ResourceKey<PostBlock.ModelType> fallbackModelType) {
        modelTypeKey = data.modelType().orElse(fallbackModelType);
        modelType = ModelTypeRegistry.getOrFallbackModelType(registryAccess, modelTypeKey, this::getBlockMaterialType);
        parts.clear();
        for(Map.Entry<UUID, BlockPartInstance> entry : data.parts().entrySet()) {
            addPart(
                entry.getKey(),
                entry.getValue(),
                ItemStack.EMPTY,
                PlayerHandle.Invalid
            );
        }
    }
    /**
     * Replaces a pre-2.04 post block with the {@link PostBlock.MaterialType} block that supersedes it, keeping
     * everything this tile holds. Runs once, a tick after the chunk was loaded, so that a world only ever
     * carries the legacy ids until the first time it is opened by this version.
     *
     * <p>The parts are moved over rather than re-attached: their listeners key off the block position, which
     * does not change, and attaching them a second time would register those listeners twice.
     */
    private void migrateLegacyBlock(ServerLevel level) {
        if (isRemoved()) return;
        var pos = getBlockPos();
        var oldState = level.getBlockState(pos);
        if (!(oldState.getBlock() instanceof PostBlock oldBlock) || !oldBlock.isLegacy()) return;

        var newState = oldBlock.materialType.getBlock().defaultBlockState()
            .setValue(PostBlock.Facing, oldState.getValue(PostBlock.Facing))
            .setValue(PostBlock.WATERLOGGED, oldState.getValue(PostBlock.WATERLOGGED));
        var migratedParts = parts;
        var migratedOwner = owner;
        var migratedModelType = modelTypeKey();

        level.setBlock(pos, newState, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof PostTile migrated) {
            migrated.modelTypeKey = migratedModelType;
            migrated.modelType = null;
            migrated.parts = migratedParts;
            migrated.owner = migratedOwner;
            migrated.setChanged();
            level.sendBlockUpdated(pos, newState, newState, Block.UPDATE_ALL);
        } else Signpost.LOGGER.error(
            "Failed to migrate the signpost at {} from {} to {}: no block entity was created for the new block.",
            pos, oldState.getBlock(), newState.getBlock());
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);

        if(level instanceof ServerLevel migrationLevel && postBlock().isLegacy())
            IDelay.forFrames(1, false, () -> migrateLegacyBlock(migrationLevel));

        if(!IConfig.IServer.getInstance().worldGen().debugMode() && level instanceof ServerLevel serverLevel) {
            IDelay.forFrames(1, false, () -> {
                if (isRemoved()) return; // superseded by migrateLegacyBlock; the new tile runs this itself
                boolean hasChanged = false;
                for(var e : parts.entrySet().stream().sorted((e1, e2) -> Float.compare(e2.getValue().offset().y(), e1.getValue().offset().y())).toList()) {
                    if (e.getValue().blockPart() instanceof SignBlockPart<?> sign
                        && sign.isMarkedForGeneration()
                        && VillageSignpost.populate(this, sign, e.getKey(), e.getValue().offset().y(), serverLevel)
                    ) hasChanged = true;
                }
                if(hasChanged) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
            });
        }

        toDoOnceLevelIsSet.forEach(Runnable::run);
        toDoOnceLevelIsSet.clear();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
        writeSelf(output);
        return output.buildResult();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void notifyMutation(UUID part, BlockPartInstance data, String partMetaIdentifier) {
        sendToTracing(
            () -> new PostTile.PartMutatedEvent.Packet(
                new PostTile.TilePartInfo(this, part),
                data.blockPart(),
                partMetaIdentifier,
                Optional.empty())
        );
        setChanged();
    }

    public <T> void sendToTracing(Supplier<T> t) {
        PacketHandler.getInstance().sendToTracing(this, t);
    }

    public void setSignpostOwner(Optional<PlayerHandle> owner) {
        this.owner = owner;
    }

    public Optional<PlayerHandle> getSignpostOwner() { return owner; }

    public Optional<WaystoneBlockPart> getWaystonePart() {
        return getParts().stream().filter(p -> p.blockPart() instanceof WaystoneBlockPart).findFirst()
            .map(p -> (WaystoneBlockPart) p.blockPart());
    }

    public Optional<PlayerHandle> getWaystoneOwner() {
        return getWaystonePart().flatMap(WaystoneBlockPart::getWaystoneOwner);
    }

    @Override
    public void setWaystoneOwner(Optional<PlayerHandle> owner) {
        getWaystonePart().ifPresent(part -> part.setWaystoneOwner(owner));
    }

    public static boolean isAngleTool(Item item) {
        return item instanceof Wrench;
    }

    public Optional<BlockPartInstance> getPart(UUID id) {
        return parts.containsKey(id) ? Optional.of(parts.get(id)) : Optional.empty();
    }

    public static class TilePartInfo {
        public final Identifier dimensionKey;
        public final BlockPos pos;
        public final UUID identifier;

        public TilePartInfo(BlockEntity tile, UUID identifier) {
            this.dimensionKey = tile.getLevel().dimension().identifier();
            this.pos = tile.getBlockPos();
            this.identifier = identifier;
        }

        public TilePartInfo(Identifier dimensionKey, BlockPos pos, UUID identifier) {
            this.dimensionKey = dimensionKey;
            this.pos = pos;
            this.identifier = identifier;
        }

        public static final Codec<TilePartInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("Dimension").forGetter(t -> t.dimensionKey),
            BlockPosSerializer.CODEC.fieldOf("Pos").forGetter(t -> t.pos),
            UUIDUtil.CODEC.fieldOf("Id").forGetter(t -> t.identifier)
        ).apply(i, TilePartInfo::new));

        public static final StreamCodec<ByteBuf, TilePartInfo> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, t -> t.dimensionKey,
            BlockPos.STREAM_CODEC, t -> t.pos,
            UUIDUtil.STREAM_CODEC, t -> t.identifier,
            TilePartInfo::new
        );

    }

    public static class PartAddedEvent implements PacketHandler.Event<PartAddedEvent.Packet> {

        public record Packet(
            TilePartInfo info,
            BlockPartInstance part,
            ItemStack cost,
            PlayerHandle player
        ) {
            public static final StreamCodec<RegistryFriendlyByteBuf, Packet> STREAM_CODEC = StreamCodec.composite(
                TilePartInfo.STREAM_CODEC, Packet::info,
                BlockPartInstance.STREAM_CODEC, Packet::part,
                ItemStack.OPTIONAL_STREAM_CODEC, Packet::cost,
                PlayerHandle.STREAM_CODEC, Packet::player,
                Packet::new
            );
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Packet> codec() {
            return Packet.STREAM_CODEC;
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void handle(Packet message, PacketHandler.Context context) {
            boolean isClientSide = context instanceof PacketHandler.Context.Client;
            TileEntityUtils.findTileEntity(
                message.info.dimensionKey,
                isClientSide,
                message.info.pos,
                PostTile.getBlockEntityType()
            ).ifPresent(tile -> {
                tile.addPart(message.info.identifier, message.part, message.cost, message.player);
                if(message.cost.getCount() > 0 &&
                       (!isClientSide ||
                            (SideUtils.getClientPlayer().map(player -> player.getUUID().equals(message.player.id())).orElse(false)))) {
                    SideUtils.makePlayerPayIfEditor(
                        isClientSide,
                        context instanceof PacketHandler.Context.Server(ServerPlayer sender)
                            ? sender
                            : null,
                        message.player,
                        message.cost
                    );
                }
                tile.setChanged();
            });
        }

    }

    public static class PartRemovedEvent implements PacketHandler.Event<PartRemovedEvent.Packet> {

        public record Packet(TilePartInfo info, boolean shouldDropItem) {
            public static final StreamCodec<RegistryFriendlyByteBuf, Packet> STREAM_CODEC = StreamCodec.composite(
                TilePartInfo.STREAM_CODEC, Packet::info,
                ByteBufCodecs.BOOL, Packet::shouldDropItem,
                Packet::new
            );
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Packet> codec() {
            return Packet.STREAM_CODEC;
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void handle(Packet message, PacketHandler.Context context) {
            boolean isClient = context instanceof PacketHandler.Context.Client;
            TileEntityUtils.findWorld(message.info.dimensionKey, isClient).ifPresent(level ->
                TileEntityUtils.delayUntilTileEntityExistsAt(
                    WorldLocation.from(message.info.pos, level),
                    PostTile.class,
                    tile -> {
                        BlockPartInstance oldPart = tile.removePart(message.info.identifier);
                        if (oldPart != null
                            && context instanceof PacketHandler.Context.Server(ServerPlayer sender)
                            && !sender.isCreative() && message.shouldDropItem
                        ) {
                            for(ItemStack item : ((BlockPart<?>) oldPart.blockPart()).getDrops()) {
                                if(!sender.getInventory().add(item))
                                    if(tile.getLevel() instanceof ServerLevel serverWorld) {
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

        public static record Packet(TilePartInfo info, BlockPart blockPart, String partMetaIdentifier, Optional<Vector3> offset) {

            public Packet(TilePartInfo info, BlockPart blockPart, String partMetaIdentifier) {
                this(info, blockPart, partMetaIdentifier, Optional.empty());
            }

            public Packet(TilePartInfo info, BlockPart blockPart, String partMetaIdentifier, Vector3 offset) {
                this(info, blockPart, partMetaIdentifier, Optional.of(offset));
            }

            public static final StreamCodec<RegistryFriendlyByteBuf, Packet> STREAM_CODEC = StreamCodec.composite(
                TilePartInfo.STREAM_CODEC, Packet::info,
                BlockPart.STREAM_CODEC, Packet::blockPart,
                ByteBufCodecs.STRING_UTF8, Packet::partMetaIdentifier,
                ByteBufCodecs.optional(Vector3.STREAM_CODEC), Packet::offset,
                Packet::new
            );
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Packet> codec() {
            return Packet.STREAM_CODEC;
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void handle(@NotNull Packet message, PacketHandler.Context context) {
            boolean isServer = context instanceof PacketHandler.Context.Server;
            TileEntityUtils.findWorld(message.info.dimensionKey, !isServer).ifPresent(level ->
                TileEntityUtils.delayUntilTileEntityExistsAt(
                    WorldLocation.from(message.info.pos, level),
                    PostTile.class,
                    tile -> {
                        var oldPart = tile.parts.get(message.info.identifier);
                        var offset = message.offset.orElse(oldPart != null ? oldPart.offset() : Vector3.ZERO);
                        if (oldPart != null) {
                            oldPart.blockPart().removeFrom(tile);
                        } else {
                            Signpost.LOGGER.error("Tried to mutate a post part that wasn't present: " + message.info.identifier);
                        }
                        tile.parts.put(message.info.identifier, new BlockPartInstance(message.blockPart, offset));
                        message.blockPart().attachTo(tile);
                        tile.setChanged();
                        if(isServer)
                            tile.sendToTracing(() -> message);
                    },
                    100,
                    !isServer,
                    Optional.of(() -> Signpost.LOGGER.error("Failed to process PartMutatedEvent, tile was not present"))
                ));
        }
    }

    public static class UpdateAllPartsEvent implements PacketHandler.Event<UpdateAllPartsEvent.Packet> {

        public record Packet(CompoundTag tag, WorldLocation location) {
            public static Packet from(CompoundTag tag, WorldLocation location) {
                return new Packet(
                    tag,
                    location.withoutExplicitLevel()
                );
            }

            public static final StreamCodec<RegistryFriendlyByteBuf, Packet> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.compoundTagCodec(NbtAccounter::unlimitedHeap), Packet::tag,
                WorldLocation.STREAM_CODEC, Packet::location,
                Packet::new
            );
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Packet> codec() {
            return Packet.STREAM_CODEC;
        }

        @Override
        public Class<Packet> getMessageClass() {
            return Packet.class;
        }

        @Override
        public void handle(Packet message, PacketHandler.Context context) {
            if(context instanceof PacketHandler.Context.Client)
                TileEntityUtils.delayUntilTileEntityExistsAt(
                    message.location,
                    PostTile.getBlockEntityType(),
                    tile -> tile.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, context.getHolderLookupProvider(), message.tag)),
                    20,
                    true,
                    Optional.empty()
                );
        }
    }

}
