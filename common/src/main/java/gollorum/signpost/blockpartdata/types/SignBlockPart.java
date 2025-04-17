package gollorum.signpost.blockpartdata.types;

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
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.ItemStackSerializer;
import gollorum.signpost.utils.serialization.OptionalCompoundSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public abstract class SignBlockPart<Self extends SignBlockPart<Self>> implements BlockPart<Self> {

    protected static final class CoreData {
        public AngleProvider angleProvider;
        public boolean flip;
        public Texture mainTexture;
        public Texture secondaryTexture;
        public Optional<Overlay> overlay;
        public int color;
        public Optional<WaystoneHandle> destination;
        public PostBlock.ModelType modelType;
        public ItemStack itemToDropOnBreak;
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
            ItemStack itemToDropOnBreak,
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

        public static final Serializer SERIALIZER = new Serializer();
        public static final class Serializer implements CompoundSerializable<CoreData> {
            private Serializer(){}
            @Override
            public void encode(CompoundTag compound, CoreData coreData, HolderLookup.Provider provider) {
                compound.put("Angle", AngleProvider.CompoundSerializer.encode(coreData.angleProvider, provider));
                compound.putBoolean("Flip", coreData.flip);
                compound.put("Texture", Texture.CompundSerializer.encode(coreData.mainTexture, provider));
                compound.put("TextureDark", Texture.CompundSerializer.encode(coreData.secondaryTexture, provider));
                compound.put("Overlay", Overlay.CompoundSerializer.optional().encode(coreData.overlay, provider));
                compound.putInt("Color", coreData.color);

                CompoundTag dest = new CompoundTag();
                dest.putBoolean("IsPresent", coreData.destination.isPresent());
                coreData.destination.ifPresent(d -> d.write(dest, provider));
                compound.put("Destination", dest);

                compound.put("ItemToDropOnBreak", ItemStackSerializer.Compound.encode(coreData.itemToDropOnBreak, provider));
                compound.putString("ModelType", coreData.modelType.name);
                compound.putBoolean("IsLocked", coreData.isLocked);
                compound.putBoolean("IsMarkedForGeneration", coreData.isMarkedForGeneration);
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
                    && compound.contains("IsLocked")
                    && compound.contains("IsMarkedForGeneration");
            }

            @Override
            public CoreData decode(CompoundTag compound, HolderLookup.Provider provider) {
                CompoundTag dest = compound.getCompound("Destination");
                Optional<WaystoneHandle> destination;
                if(dest.getBoolean("IsPresent")){
                    Optional<WaystoneHandle> d2 = WaystoneHandle.read(dest, provider);
                    if(!d2.isPresent()) Signpost.LOGGER.error("Error deserializing waystone handle of unknown type: " + dest.getString("type"));
                    destination = d2;
                } else destination = Optional.empty();
                return new CoreData(
                    AngleProvider.fetchFrom(compound.getCompound("Angle"), provider),
                    compound.getBoolean("Flip"),
                    Texture.readFrom(compound.get("Texture"), provider),
                    Texture.readFrom(compound.get("TextureDark"), provider),
                    Overlay.CompoundSerializer.optional().decode(compound.getCompound("Overlay"), provider),
                    compound.getInt("Color"),
                    destination,
                    PostBlock.ModelType.getByName(compound.getString("ModelType"), true)
                        .orElseThrow(() -> new RuntimeException("Tried to load sign post model type " + compound.getString("ModelType") +
                            ", but it hasn't been registered. @Dev: You have to call Post.ModelType.register")),
                    ItemStackSerializer.Compound.decode(compound.getCompound("ItemToDropOnBreak"), provider),
                   compound.getBoolean("IsLocked"),
                   compound.getBoolean("IsMarkedForGeneration")
                );
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
        BlockPos myBlockPos = tile.getBlockPos();
        BlockPartWaystoneUpdateListener.getInstance().addListener((Self)this,
            (self, event) -> onWaystoneUpdated(myBlockPos, self, event));
    }

    private static void onWaystoneUpdated(BlockPos myBlockPos, SignBlockPart<?> self, WaystoneUpdatedEvent event) {
        self.getDestination().ifPresent(handle -> {
            if (handle.equals(event.handle)) {
                if (self.getAngle() instanceof AngleProvider.WaystoneTarget)
                    ((AngleProvider.WaystoneTarget) self.getAngle()).setCachedAngle(
                        pointingAt(myBlockPos, event.location.block.blockPos));
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

    public boolean isMarkedForGeneration() { return coreData.isMarkedForGeneration; }

    public boolean hasThePermissionToEdit(WithOwner tile, @Nullable Player player) {
        return !(tile instanceof WithOwner.OfSignpost) || !coreData.isLocked || player == null
            || ((WithOwner.OfSignpost)tile).getSignpostOwner().map(o -> o.id.equals(player.getUUID())).orElse(true)
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
                    notifyFlipChanged(info);
                } else {
                    Vector3 diff = info.traceResult.ray.start.negated().add(0.5f, 0.5f, 0.5f).withY(0).normalized();
                    Vector3 rayDir = info.traceResult.ray.dir.withY(0).normalized();
                    Angle angleToPost = Angle.between(rayDir.x(), rayDir.z(), diff.x(), diff.z()).normalized();
                    setAngle(new AngleProvider.Literal(coreData.angleProvider
                        .get().add(Angle.fromDegrees(angleToPost.radians() < 0 ? 15 : -15))));
                    notifyAngleChanged(info);
                }
            } else if(isGenerationWand(heldItem)) {
                coreData.isMarkedForGeneration = !coreData.isMarkedForGeneration;
                notifyMarkedForGenerationChanged(info);
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
                        int distance = (int) data.loc().spawn.distanceTo(Vector3.fromVec3d(player.position()));
                        return new Teleport.RequestGui.Package.Info(
                            IConfig.IServer.getInstance().teleport().maximumDistance(),
                            distance,
                            cannotTeleportBecause,
                            data.name(),
                            Teleport.getCost(player, Vector3.fromVec3d(player.position()), data.loc().spawn),
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
        return item instanceof Brush;
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

    protected void notifyAngleChanged(InteractionInfo info) {
        CompoundTag compound = new CompoundTag();
        compound.put("Angle", AngleProvider.CompoundSerializer.encode(coreData.angleProvider, info.player.registryAccess()));
        info.mutationDistributor.accept(compound);
    }

    protected void notifyTextureChanged(InteractionInfo info) {
        CompoundTag compound = new CompoundTag();
        compound.put("Texture", Texture.CompundSerializer.encode(coreData.mainTexture, info.player.registryAccess()));
        compound.put("TextureDark", Texture.CompundSerializer.encode(coreData.secondaryTexture, info.player.registryAccess()));
        info.mutationDistributor.accept(compound);
    }

    protected void notifyFlipChanged(InteractionInfo info) {
        CompoundTag compound = new CompoundTag();
        compound.putBoolean("Flip", coreData.flip);
        info.mutationDistributor.accept(compound);
    }

    protected void notifyMarkedForGenerationChanged(InteractionInfo info) {
        CompoundTag compound = new CompoundTag();
        compound.putBoolean("IsMarkedForGeneration", coreData.isMarkedForGeneration);
        info.mutationDistributor.accept(compound);
    }

    @Override
    public void readMutationUpdate(CompoundTag compound, BlockEntity tile, @Nullable Player editingPlayer, HolderLookup.Provider provider) {
        if(compound.contains("CoreData")) compound = compound.getCompound("CoreData");
        if(compound.contains("Angle"))
            setAngle(AngleProvider.fetchFrom(compound.getCompound("Angle"), provider));

        boolean updateTextures = false;
        if(compound.contains("Texture")) {
            coreData.mainTexture = Texture.readFrom(compound.get("Texture"), provider);
            updateTextures = true;
        }
        if(compound.contains("TextureDark")){
            coreData.secondaryTexture = Texture.readFrom(compound.get("TextureDark"), provider);
            updateTextures = true;
        }
        if(updateTextures) setTextures(coreData.mainTexture, coreData.secondaryTexture);

        if(compound.contains("Flip")) setFlip(compound.getBoolean("Flip"));
        if(compound.contains("Color")) setColor(compound.getInt("Color"));
        if(compound.contains("Destination")) {
            CompoundTag dest = compound.getCompound("Destination");
            Optional<WaystoneHandle> destination;
            if(dest.getBoolean("IsPresent")){
                Optional<WaystoneHandle> d2 = WaystoneHandle.read(dest, provider);
                if (d2.isPresent()) {
                    setDestination(d2);
                } else {
                    Signpost.LOGGER.error("Error deserializing waystone handle of unknown type: " + dest.getString("type"));
                }
            } else setDestination(Optional.empty());
        }
        if(compound.contains("ItemToDropOnBreak")) {
            setItemToDropOnBreak(ItemStackSerializer.Compound.decode(compound.getCompound("ItemToDropOnBreak"), provider));
        }
        if(compound.contains("ModelType"))
            PostBlock.ModelType.getByName(compound.getString("ModelType"), true).ifPresent(this::setModelType);

        OptionalCompoundSerializer<Overlay> overlaySerializer = Overlay.CompoundSerializer.optional();
        if(compound.contains("Overlay"))
            setOverlay(overlaySerializer.decode(compound.getCompound("Overlay"), provider));

        if(compound.contains("IsLocked")) {
            if(editingPlayer == null || editingPlayer.level().isClientSide()
                || ((WithOwner.OfSignpost)tile).getSignpostOwner().map(owner -> editingPlayer.getUUID().equals(owner.id)).orElse(true)
                || editingPlayer.hasPermissions(IConfig.IServer.getInstance().permissions().editLockedSignCommandPermissionLevel()))
                coreData.isLocked = compound.getBoolean("IsLocked");
        }
        if(compound.contains("IsMarkedForGeneration"))
            coreData.isMarkedForGeneration = compound.getBoolean("IsMarkedForGeneration");

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
    public Collection<Texture> getAllTextures() {
        return Arrays.asList(getMainTexture(), getSecondaryTexture());
    }
}
