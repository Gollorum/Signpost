package gollorum.signpost.blockpartdata.types;

import gollorum.signpost.*;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.renderers.BlockPartWaystoneUpdateListener;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.gui.PaintSignGui;
import gollorum.signpost.minecraft.gui.RequestSignGui;
import gollorum.signpost.minecraft.items.Brush;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.relations.ExternalWaystone;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.BlockPart;
import gollorum.signpost.utils.Either;
import gollorum.signpost.utils.NameProvider;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.AngleProvider;
import gollorum.signpost.utils.math.geometry.Intersectable;
import gollorum.signpost.utils.math.geometry.Ray;
import gollorum.signpost.utils.math.geometry.TransformedBox;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.ItemStackSerializer;
import gollorum.signpost.utils.serialization.OptionalSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class SignBlockPart<Self extends SignBlockPart<Self>> implements BlockPart<Self> {

    protected static final class CoreData {
        public AngleProvider angleProvider;
        public boolean flip;
        public ResourceLocation mainTexture;
        public ResourceLocation secondaryTexture;
        public Optional<Overlay> overlay;
        public int color;
        public Optional<WaystoneHandle> destination;
        public PostBlock.ModelType modelType;
        public ItemStack itemToDropOnBreak;
        public boolean isLocked;

        public CoreData(
            AngleProvider angleProvider,
            boolean flip,
            ResourceLocation mainTexture,
            ResourceLocation secondaryTexture,
            Optional<Overlay> overlay,
            int color,
            Optional<WaystoneHandle> destination,
            PostBlock.ModelType modelType,
            ItemStack itemToDropOnBreak,
            boolean isLocked
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
        }

        public CoreData copy() {
            return new CoreData(
                angleProvider, flip, mainTexture, secondaryTexture, overlay, color, destination, modelType, itemToDropOnBreak, isLocked
            );
        }

        public static final Serializer SERIALIZER = new Serializer();
        public static final class Serializer implements CompoundSerializable<CoreData> {
            private Serializer(){}
            @Override
            public CompoundTag write(CoreData coreData, CompoundTag compound) {
                compound.put("Angle", AngleProvider.Serializer.write(coreData.angleProvider));
                compound.putBoolean("Flip", coreData.flip);
                compound.putString("Texture", coreData.mainTexture.toString());
                compound.putString("TextureDark", coreData.secondaryTexture.toString());
                compound.put("Overlay", Overlay.Serializer.optional().write(coreData.overlay));
                compound.putInt("Color", coreData.color);

                CompoundTag dest = new CompoundTag();
                dest.putBoolean("IsPresent", coreData.destination.isPresent());
                coreData.destination.ifPresent(d -> d.write(dest));
                compound.put("Destination", dest);

                compound.put("ItemToDropOnBreak", ItemStackSerializer.Instance.write(coreData.itemToDropOnBreak));
                compound.putString("ModelType", coreData.modelType.name);
                compound.putBoolean("IsLocked", coreData.isLocked);
                return compound;
            }

            @Override
            public boolean isContainedIn(CompoundTag compound) {
                return compound.contains("Angle")
                    && compound.contains("Flip")
                    && compound.contains("Texture")
                    && compound.contains("TextureDark")
                    && compound.contains("Overlay")
                    && compound.contains("Color")
                    && compound.contains("Destination")
                    && compound.contains("ItemToDropOnBreak")
                    && compound.contains("IsLocked");
            }

            @Override
            public CoreData read(CompoundTag compound) {
                CompoundTag dest = compound.getCompound("Destination");
                Optional<WaystoneHandle> destination;
                if(dest.getBoolean("IsPresent")){
                    Optional<WaystoneHandle> d2 = WaystoneHandle.read(dest);
                    if(!d2.isPresent()) Signpost.LOGGER.error("Error deserializing waystone handle of unknown type: " + dest.getString("type"));
                    destination = d2;
                } else destination = Optional.empty();
                return new CoreData(
                    AngleProvider.fetchFrom(compound.getCompound("Angle")),
                    compound.getBoolean("Flip"),
                    new ResourceLocation(compound.getString("Texture")),
                    new ResourceLocation(compound.getString("TextureDark")),
                    Overlay.Serializer.optional().read(compound.getCompound("Overlay")),
                    compound.getInt("Color"),
                    destination,
                    PostBlock.ModelType.getByName(compound.getString("ModelType"), true)
                        .orElseThrow(() -> new RuntimeException("Tried to load sign post model type " + compound.getString("ModelType") +
                            ", but it hasn't been registered. @Dev: You have to call Post.ModelType.register")),
                    ItemStackSerializer.Instance.read(compound.getCompound("ItemToDropOnBreak")),
                   compound.getBoolean("IsLocked")
                );
            }

            @Override
            public Class<CoreData> getTargetClass() {
                return CoreData.class;
            }
        }
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
        BlockPartWaystoneUpdateListener.getInstance().addListener(this, event ->
            getDestination().ifPresent(handle -> {
                if (handle.equals(event.handle)) {
                    if (getAngle() instanceof AngleProvider.WaystoneTarget)
                        ((AngleProvider.WaystoneTarget) getAngle()).setCachedAngle(
                            pointingAt(tile.getBlockPos(), event.location.block.blockPos));
                    for(NameProvider np : getNameProviders())
                        if(np instanceof NameProvider.WaystoneTarget)
                            ((NameProvider.WaystoneTarget)np).setCachedName(event.name);
                }
            }));
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

    public void setItemToDropOnBreak(ItemStack itemToDropOnBreak) {
        coreData.itemToDropOnBreak = itemToDropOnBreak;
    }

    private void setModelType(PostBlock.ModelType modelType) {
        coreData.modelType = modelType;
    }

    public ItemStack getItemToDropOnBreak() { return coreData.itemToDropOnBreak; }

    public boolean isFlipped() { return coreData.flip; }

    public int getColor() { return coreData.color; }

    public PostBlock.ModelType getModelType() { return coreData.modelType; }

    public boolean isLocked() { return coreData.isLocked; }

    public boolean hasThePermissionToEdit(WithOwner tile, @Nullable Player player) {
        return !(tile instanceof WithOwner.OfSignpost) || !coreData.isLocked || player == null
            || ((WithOwner.OfSignpost)tile).getSignpostOwner().map(o -> o.id.equals(player.getUUID())).orElse(true)
            || player.hasPermissions(Config.Server.permissions.editLockedSignCommandPermissionLevel.get());
    }

    private void setTextures(ResourceLocation texture, ResourceLocation textureDark) {
        coreData.mainTexture = texture;
        coreData.secondaryTexture = textureDark;
    }

    public ResourceLocation getMainTexture() { return coreData.mainTexture; }
    public ResourceLocation getSecondaryTexture() { return coreData.secondaryTexture; }

    public void setMainTexture(ResourceLocation tex) {
        coreData.mainTexture = tex;
    }
    public void setSecondaryTexture(ResourceLocation tex) {
        coreData.secondaryTexture = tex;
    }

    private void setOverlay(Optional<Overlay> overlay) {
        coreData.overlay = overlay;
    }

    public Optional<Overlay> getOverlay() { return coreData.overlay; }

    protected abstract void regenerateTransformedBox();

    @Override
    public Intersectable<Ray, Float> getIntersection() {
        return transformedBounds;
    }

    @Override
    public InteractionResult interact(InteractionInfo info) {
        ItemStack heldItem = info.player.getItemInHand(info.hand);
        if (!info.isRemote) {
            if(holdsAngleTool(info)) {
                if(info.player.isCrouching()) {
                    setFlip(!isFlipped());
                    notifyFlipChanged(info);
                } else {
                    Vector3 diff = info.traceResult.ray.start.negated().add(0.5f, 0.5f, 0.5f).withY(0).normalized();
                    Vector3 rayDir = info.traceResult.ray.dir.withY(0).normalized();
                    Angle angleToPost = Angle.between(rayDir.x, rayDir.z, diff.x, diff.z).normalized();
                    setAngle(new AngleProvider.Literal(coreData.angleProvider
                        .get().add(Angle.fromDegrees(angleToPost.radians() < 0 ? 15 : -15))));
                    notifyAngleChanged(info);
                }
            } else if(!isBrush(heldItem))
                tryTeleport((ServerPlayer) info.player, info.getTilePartInfo());
        } else if(isBrush(heldItem))
            paint(info);
        return InteractionResult.Accepted;
    }

    private void tryTeleport(ServerPlayer player, PostTile.TilePartInfo tilePartInfo) {
        if(Config.Server.teleport.enableTeleport.get() && coreData.destination.isPresent() && (!(coreData.destination.get() instanceof WaystoneHandle.Vanilla) || WaystoneLibrary.getInstance().contains((WaystoneHandle.Vanilla) coreData.destination.get()))) {
            WaystoneHandle dest = coreData.destination.get();
            Optional<WaystoneHandle.Vanilla> vanillaHandle = dest instanceof WaystoneHandle.Vanilla
                ? Optional.of((WaystoneHandle.Vanilla) dest)
                : Optional.empty();
            PacketHandler.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Teleport.RequestGui.Package(
                    Either.rightIfPresent(vanillaHandle, () -> ((ExternalWaystone.Handle) dest).noTeleportLangKey())
                        .flatMapRight(h ->
                            Either.rightIfPresent(WaystoneLibrary.getInstance().getData(h), () -> LangKeys.waystoneNotFound).mapRight(data -> {
                                boolean isDiscovered = WaystoneLibrary.getInstance()
                                    .isDiscovered(new PlayerHandle(player), h) || !Config.Server.teleport.enforceDiscovery.get();
                                int distance = (int) data.location.spawn.distanceTo(Vector3.fromVec3d(player.position()));
                                return new Teleport.RequestGui.Package.Info(
                                    Config.Server.teleport.maximumDistance.get(),
                                    distance,
                                    isDiscovered,
                                    data.name,
                                    Teleport.getCost(player, Vector3.fromBlockPos(data.location.block.blockPos), data.location.spawn)
                                );
                            })
                        ),
                    Optional.of(tilePartInfo)
                )
            );
        } else {
            PacketHandler.send(PacketDistributor.PLAYER.with(() -> player), new RequestSignGui.Package(tilePartInfo));
        }
    }

    private boolean holdsAngleTool(InteractionInfo info) {
        ItemStack itemStack = info.player.getItemInHand(info.hand);
        return !itemStack.isEmpty() && PostTile.isAngleTool(itemStack.getItem());
    }

    private static boolean isBrush(ItemStack itemStack) {
        if(itemStack == null || itemStack.getCount() < 1) return false;
        Item item = itemStack.getItem();
        return item instanceof Brush;
    }

    private InteractionResult paint(InteractionInfo info) {
        if(info.isRemote) {
            PaintSignGui.display(info.tile, (Self)this, info.traceResult.id);
        }
        return InteractionResult.Accepted;
    }

    protected void notifyAngleChanged(InteractionInfo info) {
        CompoundTag compound = new CompoundTag();
        compound.put("Angle", AngleProvider.Serializer.write(coreData.angleProvider));
        info.mutationDistributor.accept(compound);
    }

    protected void notifyTextureChanged(InteractionInfo info) {
        CompoundTag compound = new CompoundTag();
        compound.putString("Texture", coreData.mainTexture.toString());
        compound.putString("TextureDark", coreData.secondaryTexture.toString());
        info.mutationDistributor.accept(compound);
    }

    protected void notifyFlipChanged(InteractionInfo info) {
        CompoundTag compound = new CompoundTag();
        compound.putBoolean("Flip", coreData.flip);
        info.mutationDistributor.accept(compound);
    }

    @Override
    public void readMutationUpdate(CompoundTag compound, BlockEntity tile, Player editingPlayer) {
        if(compound.contains("CoreData")) compound = compound.getCompound("CoreData");
        if(compound.contains("Angle"))
            setAngle(AngleProvider.fetchFrom(compound.getCompound("Angle")));

        boolean updateTextures = false;
        if(compound.contains("Texture")) {
            coreData.mainTexture = new ResourceLocation(compound.getString("Texture"));
            updateTextures = true;
        }
        if(compound.contains("TextureDark")){
            coreData.secondaryTexture = new ResourceLocation(compound.getString("TextureDark"));
            updateTextures = true;
        }
        if(updateTextures) setTextures(coreData.mainTexture, coreData.secondaryTexture);

        if(compound.contains("Flip")) setFlip(compound.getBoolean("Flip"));
        if(compound.contains("Color")) setColor(compound.getInt("Color"));
        if(compound.contains("Destination")) {
            CompoundTag dest = compound.getCompound("Destination");
            Optional<WaystoneHandle> destination;
            if(dest.getBoolean("IsPresent")){
                Optional<WaystoneHandle> d2 = WaystoneHandle.read(dest);
                if (d2.isPresent()) {
                    setDestination(d2);
                } else {
                    Signpost.LOGGER.error("Error deserializing waystone handle of unknown type: " + dest.getString("type"));
                }
            } else setDestination(Optional.empty());
        }
        if(compound.contains("ItemToDropOnBreak")) {
            setItemToDropOnBreak(ItemStackSerializer.Instance.read(compound.getCompound("ItemToDropOnBreak")));
        }
        if(compound.contains("ModelType"))
            PostBlock.ModelType.getByName(compound.getString("ModelType"), true).ifPresent(this::setModelType);

        OptionalSerializer<Overlay> overlaySerializer = Overlay.Serializer.optional();
        if(compound.contains("Overlay"))
            setOverlay(overlaySerializer.read(compound.getCompound("Overlay")));

        if(compound.contains("IsLocked")) {
            if(editingPlayer == null || editingPlayer.level.isClientSide()
                || ((WithOwner.OfSignpost)tile).getSignpostOwner().map(owner -> editingPlayer.getUUID().equals(owner.id)).orElse(true)
                || editingPlayer.hasPermissions(Config.Server.permissions.editLockedSignCommandPermissionLevel.get()))
                coreData.isLocked = compound.getBoolean("IsLocked");
        }
        tile.setChanged();
    }

    @Override
    public Collection<ItemStack> getDrops(PostTile tile) {
        return Collections.singleton(coreData.itemToDropOnBreak);
    }

    private void dropOn(Level world, BlockPos pos) {
        if(!coreData.itemToDropOnBreak.isEmpty() && !world.isClientSide()) {
            ItemEntity itementity = new ItemEntity(
                world,
                pos.getX() + world.getRandom().nextFloat() * 0.5 + 0.25,
                pos.getY() + world.getRandom().nextFloat() * 0.5 + 0.25,
                pos.getZ() + world.getRandom().nextFloat() * 0.5 + 0.25,
                coreData.itemToDropOnBreak
            );
            itementity.setDefaultPickUpDelay();
            world.addFreshEntity(itementity);
        }
    }

    public AngleProvider getAngle() {
        return coreData.angleProvider;
    }

    public abstract Self copy();

    @Override
    public Collection<ResourceLocation> getAllTextures() {
        return Arrays.asList(getMainTexture(), getSecondaryTexture());
    }
}
