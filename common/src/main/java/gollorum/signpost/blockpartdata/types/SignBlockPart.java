package gollorum.signpost.blockpartdata.types;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.*;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.renderers.BlockPartWaystoneUpdateListener;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.events.WaystoneUpdatedEvent;
import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.minecraft.gui.PaintSignGui;
import gollorum.signpost.minecraft.gui.RequestSignGui;
import gollorum.signpost.minecraft.items.Brush;
import gollorum.signpost.minecraft.items.GenerationWand;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.Texture;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.*;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Intersectable;
import gollorum.signpost.utils.math.geometry.Ray;
import gollorum.signpost.utils.math.geometry.TransformedBox;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.ItemStackSerializer;
import gollorum.signpost.utils.serialization.OptionalSerializerV1;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public abstract class SignBlockPart<Self extends SignBlockPart<Self>> implements BlockPart<Self> {

    public static final class CoreData {
        public AngleProvider angleProvider;
        public boolean flip;
        public Texture mainTexture;
        public Texture secondaryTexture;
        public Optional<Overlay> overlay;
        public int color;
        public Optional<WaystoneHandle> destination;
        public PostBlock.ModelType modelType;
        public Optional<ItemStack> itemToDropOnBreak;
        public boolean isLocked;

        // If true, the sign will not be rendered unless the config option is active
        // until a village waystone is found that it can point to.
        public boolean isMarkedForGeneration;

        public CoreData(
            AngleProvider angleProvider,
            boolean flip,
            Texture mainTexture,
            Texture secondaryTexture,
            Optional<Overlay> overlay,
            int color,
            Optional<WaystoneHandle> destination,
            PostBlock.ModelType modelType,
            Optional<ItemStack> itemToDropOnBreak,
            boolean isLocked,
            boolean isMarkedForGeneration
        ) {
            this.angleProvider = angleProvider;
            this.flip = flip;
            this.mainTexture = mainTexture;
            this.secondaryTexture = secondaryTexture;
            this.overlay = overlay;
            this.color = color;
            this.destination = destination;
            this.modelType = modelType;
            this.itemToDropOnBreak = itemToDropOnBreak;
            this.isLocked = isLocked;
            this.isMarkedForGeneration = isMarkedForGeneration;
        }

        public CoreData copy() {
            return new CoreData(
                angleProvider, flip, mainTexture, secondaryTexture, overlay, color, destination, modelType, itemToDropOnBreak, isLocked, isMarkedForGeneration
            );
        }

        public static Codec<CoreData> codec(int version) {
            var optionalOverlayCodec = version < 2
                ? OptionalSerializerV1.of(Overlay.CODEC).fieldOf("Overlay")
                : Codec.optionalField("Overlay", Overlay.CODEC, true);
            var optionalDestinationCodec = version < 2
                ? Codec.BOOL.<Optional<WaystoneHandle>>dispatch(
                    "IsPresent",
                    Optional::isPresent,
                    isPresent -> isPresent
                        ? WaystoneHandle.MAP_CODEC.xmap(Optional::of, Optional::get)
                        : MapCodec.unit(Optional.empty())
                ).fieldOf("Destination")
                : WaystoneHandle.MAP_CODEC.codec().optionalFieldOf("Destination");
            return RecordCodecBuilder.create(i -> i.group(
                AngleProvider.MAP_CODEC.fieldOf("Angle").forGetter(coreData -> coreData.angleProvider),
                Codec.BOOL.fieldOf("Flip").forGetter(coreData -> coreData.flip),
                Texture.codec(version).fieldOf("Texture").forGetter(coreData -> coreData.mainTexture),
                Texture.codec(version).fieldOf("TextureDark").forGetter(coreData -> coreData.secondaryTexture),
                optionalOverlayCodec.forGetter(coreData -> coreData.overlay),
                Codec.INT.fieldOf("Color").forGetter(coreData -> coreData.color),
                optionalDestinationCodec.forGetter(coreData -> coreData.destination),
                PostBlock.ModelType.CODEC.fieldOf("ModelType").forGetter(coreData -> coreData.modelType),
                Codec.optionalField("ItemToDropOnBreak", ItemStackSerializer.CODEC.codec(), true).forGetter(coreData -> coreData.itemToDropOnBreak),
                Codec.BOOL.fieldOf("IsLocked").forGetter(coreData -> coreData.isLocked),
                Codec.BOOL.fieldOf("IsMarkedForGeneration").forGetter(coreData -> coreData.isMarkedForGeneration)
            ).apply(i, CoreData::new));
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, CoreData> STREAM_CODEC = StreamCodec.composite(
            AngleProvider.STREAM_CODEC, coreData -> coreData.angleProvider,
            ByteBufCodecs.BOOL, coreData -> coreData.flip,
            Tuple.streamCodec(Texture.STREAM_CODEC, Texture.STREAM_CODEC), coreData -> Tuple.of(
                coreData.mainTexture, coreData.secondaryTexture
            ),
            Tuple.streamCodec(ByteBufCodecs.optional(Overlay.STREAM_CODEC), ByteBufCodecs.INT), coreData -> Tuple.of(
                coreData.overlay,
                coreData.color
            ),
            ByteBufCodecs.optional(WaystoneHandle.STREAM_CODEC), coreData -> coreData.destination,
            PostBlock.ModelType.STREAM_CODEC, coreData -> coreData.modelType,
            ByteBufCodecs.optional(ItemStack.OPTIONAL_STREAM_CODEC), coreData -> coreData.itemToDropOnBreak,
            ByteBufCodecs.BOOL, coreData -> coreData.isLocked,
            ByteBufCodecs.BOOL, coreData -> coreData.isMarkedForGeneration,
            (angleProvider, flip, txts, overlayAndColor, destination, modelType, itemToDropOnBreak, isLocked, isMarkedForGeneration) ->
                new CoreData(
                    angleProvider, flip, txts._1(), txts._2(), overlayAndColor._1(), overlayAndColor._2(), destination, modelType, itemToDropOnBreak, isLocked, isMarkedForGeneration
                )
        );
    }

    public static Angle pointingAt(BlockPos block, BlockPos target) {
        BlockPos diff = target.subtract(block);
        return Angle.between(diff.getX(), diff.getZ(), 1, 0);
    }

    protected CoreData coreData;
    public Optional<WaystoneHandle> getDestination() { return coreData.destination; }

    protected TransformedBox transformedBounds;

    protected SignBlockPart(CoreData coreData) {
        this.coreData = coreData;
        setAngle(coreData.angleProvider);
        setTextures(coreData.mainTexture, coreData.secondaryTexture);
        setOverlay(coreData.overlay);
        setFlip(coreData.flip);
    }

    public void setAngle(AngleProvider angleProvider) {
        coreData.angleProvider = angleProvider;
        regenerateTransformedBox();
    }

    protected abstract NameProvider[] getNameProviders();

    @Override public void attachTo(PostTile tile) {
        BlockPos myBlockPos = tile.getBlockPos();
        BlockPartWaystoneUpdateListener.getInstance().addListener((Self)this,
            (self, event) -> onWaystoneUpdated(myBlockPos, self, event));
    }

    private static void onWaystoneUpdated(BlockPos myBlockPos, SignBlockPart<?> self, WaystoneUpdatedEvent event) {
        self.getDestination().ifPresent(handle -> {
            if (handle.equals(event.handle)) {
                if (self.getAngle() instanceof AngleProvider.WaystoneTarget)
                    ((AngleProvider.WaystoneTarget) self.getAngle()).setCachedAngle(
                        pointingAt(myBlockPos, event.location.block().blockPos()));
                for(NameProvider np : self.getNameProviders())
                    if(np instanceof NameProvider.WaystoneTarget)
                        ((NameProvider.WaystoneTarget)np).setCachedName(event.name);
            }
        });
    }

    public void setFlip(boolean flip) {
        coreData.flip = flip;
        setTextures(coreData.mainTexture, coreData.secondaryTexture);
        setOverlay(coreData.overlay);
        regenerateTransformedBox();
    }

    public void setColor(int color) {
        coreData.color = color;
    }

    public void setDestination(Optional<WaystoneHandle> destination) {
        coreData.destination = destination;
    }

    public void setItemToDropOnBreak(Optional<ItemStack> itemToDropOnBreak) {
        coreData.itemToDropOnBreak = itemToDropOnBreak;
    }

    private void setModelType(PostBlock.ModelType modelType) {
        coreData.modelType = modelType;
    }

    public Optional<ItemStack> getItemToDropOnBreak() { return coreData.itemToDropOnBreak; }

    public boolean isFlipped() { return coreData.flip; }

    public int getColor() { return coreData.color; }

    public PostBlock.ModelType getModelType() { return coreData.modelType; }

    public boolean isLocked() { return coreData.isLocked; }

    public boolean isMarkedForGeneration() { return coreData.isMarkedForGeneration; }

    public boolean hasThePermissionToEdit(WithOwner tile, Player player) {
        return !(tile instanceof WithOwner.OfSignpost) || !coreData.isLocked || player == null
            || ((WithOwner.OfSignpost)tile).getSignpostOwner().map(o -> o.id().equals(player.getUUID())).orElse(true)
            || player.hasPermissions(IConfig.IServer.getInstance().permissions().editLockedSignCommandPermissionLevel());
    }

    private void setTextures(Texture texture, Texture textureDark) {
        coreData.mainTexture = texture;
        coreData.secondaryTexture = textureDark;
    }

    public Texture getMainTexture() { return coreData.mainTexture; }
    public Texture getSecondaryTexture() { return coreData.secondaryTexture; }

    public void setMainTexture(Texture tex) {
        coreData.mainTexture = tex;
    }
    public void setSecondaryTexture(Texture tex) {
        coreData.secondaryTexture = tex;
    }

    private void setOverlay(Optional<Overlay> overlay) {
        coreData.overlay = overlay;
    }

    public Optional<Overlay> getOverlay() { return coreData.overlay; }

    protected abstract void regenerateTransformedBox();

    @Override
    public Intersectable<Ray, Float> getIntersection() {
        return isMarkedForGeneration() && !IConfig.IServer.getInstance().worldGen().debugMode() ? new Intersectable.Not<>() : transformedBounds;
    }

    @Override
    public InteractionResult interact(InteractionInfo info) {
        ItemStack heldItem = info.player.getItemInHand(info.hand);
        if (!info.isRemote) {
            if(holdsAngleTool(info)) {
                if(info.player.isCrouching()) {
                    setFlip(!isFlipped());
                    notifyChange(info);
                } else {
                    Vector3 diff = info.traceResult.ray.start.negated().add(0.5f, 0.5f, 0.5f).withY(0).normalized();
                    Vector3 rayDir = info.traceResult.ray.dir.withY(0).normalized();
                    Angle angleToPost = Angle.between(rayDir.x(), rayDir.z(), diff.x(), diff.z()).normalized();
                    setAngle(new AngleProvider.Literal(coreData.angleProvider
                        .get().add(Angle.fromDegrees(angleToPost.radians() < 0 ? 15 : -15))));
                    notifyChange(info);
                }
            } else if(isGenerationWand(heldItem)) {
                coreData.isMarkedForGeneration = !coreData.isMarkedForGeneration;
                notifyChange(info);
            } else if(!isBrush(heldItem))
                tryTeleport((ServerPlayer) info.player, info.getTilePartInfo());
        } else if(isBrush(heldItem))
            paint(info);
        return InteractionResult.Accepted;
    }

    private void tryTeleport(ServerPlayer player, PostTile.TilePartInfo tilePartInfo) {
        if(IConfig.IServer.getInstance().teleport().enableTeleport() && coreData.destination.isPresent() && (!(coreData.destination.get() instanceof WaystoneHandle.Vanilla) || WaystoneLibrary.getInstance().contains((WaystoneHandle.Vanilla) coreData.destination.get()))) {
            WaystoneHandle dest = coreData.destination.get();
            PacketHandler.getInstance().sendToPlayer(
                player,
                new Teleport.RequestGui.Package(
                    Either.rightIfPresent(WaystoneLibrary.getInstance().getData(dest), () -> LangKeys.waystoneNotFound).mapRight(data -> {
                        Optional<Component> cannotTeleportBecause = WaystoneHandleUtils.cannotTeleportToBecause(player, dest, data.name());
                        int distance = (int) data.loc().spawn().distanceTo(Vector3.fromVec3d(player.position()));
                        return new Teleport.RequestGui.Package.Info(
                            IConfig.IServer.getInstance().teleport().maximumDistance(),
                            distance,
                            cannotTeleportBecause,
                            data.name(),
                            Teleport.getCost(player, Vector3.fromVec3d(player.position()), data.loc().spawn()),
                            Optional.of(data.handle())
                        );
                    }),
                    Optional.of(tilePartInfo)
                )
            );
        } else {
            PacketHandler.getInstance().sendToPlayer(player, new RequestSignGui.Package(tilePartInfo));
        }
    }

    private boolean holdsAngleTool(InteractionInfo info) {
        ItemStack itemStack = info.player.getItemInHand(info.hand);
        return !itemStack.isEmpty() && PostTile.isAngleTool(itemStack.getItem());
    }

    private static boolean isBrush(ItemStack itemStack) {
        if(itemStack == null || itemStack.getCount() < 1) return false;
        Item item = itemStack.getItem();
        return item instanceof Brush || item instanceof BrushItem;
    }

    private static boolean isGenerationWand(ItemStack itemStack) {
        if(itemStack == null || itemStack.getCount() < 1) return false;
        Item item = itemStack.getItem();
        return item instanceof GenerationWand;
    }

    private InteractionResult paint(InteractionInfo info) {
        if(info.isRemote) {
            PaintSignGui.display(info.tile, (Self)this, info.traceResult.id);
        }
        return InteractionResult.Accepted;
    }

    protected void notifyChange(InteractionInfo info) {
        info.mutationDistributor.run();
    }

    @Override
    public Collection<ItemStack> getDrops() {
        return coreData.itemToDropOnBreak.stream().toList();
    }

    public AngleProvider getAngle() {
        return coreData.angleProvider;
    }

    public abstract Self copy();

    @Override
    public Collection<Texture> getAllTextures() {
        return Arrays.asList(getMainTexture(), getSecondaryTexture());
    }
}
