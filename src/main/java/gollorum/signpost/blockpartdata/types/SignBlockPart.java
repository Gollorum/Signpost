package gollorum.signpost.blockpartdata.types;

import gollorum.signpost.*;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.gui.PaintSignGui;
import gollorum.signpost.minecraft.gui.RequestSignGui;
import gollorum.signpost.minecraft.items.Brush;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.relations.ExternalWaystone;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.BlockPart;
import gollorum.signpost.utils.Either;
import gollorum.signpost.utils.WaystoneData;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Intersectable;
import gollorum.signpost.utils.math.geometry.Ray;
import gollorum.signpost.utils.math.geometry.TransformedBox;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.ItemStackSerializer;
import gollorum.signpost.utils.serialization.OptionalSerializer;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public abstract class SignBlockPart<Self extends SignBlockPart<Self>> implements BlockPart<Self> {

    protected static final class CoreData {
        public Angle angle;
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
            Angle angle,
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
            this.angle = angle;
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
                angle, flip, mainTexture, secondaryTexture, overlay, color, destination, modelType, itemToDropOnBreak, isLocked
            );
        }

        public static final Serializer SERIALIZER = new Serializer();
        public static final class Serializer implements CompoundSerializable<CoreData> {
            private Serializer(){}
            @Override
            public CompoundNBT write(CoreData coreData, CompoundNBT compound) {
                compound.put("Angle", Angle.Serializer.write(coreData.angle));
                compound.putBoolean("Flip", coreData.flip);
                compound.putString("Texture", coreData.mainTexture.toString());
                compound.putString("TextureDark", coreData.secondaryTexture.toString());
                compound.put("Overlay", Overlay.Serializer.optional().write(coreData.overlay));
                compound.putInt("Color", coreData.color);

                CompoundNBT dest = new CompoundNBT();
                dest.putBoolean("IsPresent", coreData.destination.isPresent());
                coreData.destination.ifPresent(d -> d.write(dest));
                compound.put("Destination", dest);

                compound.put("ItemToDropOnBreak", ItemStackSerializer.Instance.write(coreData.itemToDropOnBreak));
                compound.putString("ModelType", coreData.modelType.name);
                compound.putBoolean("IsLocked", coreData.isLocked);
                return compound;
            }

            @Override
            public boolean isContainedIn(CompoundNBT compound) {
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
            public CoreData read(CompoundNBT compound) {
                CompoundNBT dest = compound.getCompound("Destination");
                Optional<WaystoneHandle> destination;
                if(dest.getBoolean("IsPresent")){
                    Optional<WaystoneHandle> d2 = WaystoneHandle.read(dest);
                    if(!d2.isPresent()) Signpost.LOGGER.error("Error deserializing waystone handle of unknown type: " + dest.getString("type"));
                    destination = d2;
                } else destination = Optional.empty();
                return new CoreData(
                    Angle.Serializer.read(compound.getCompound("Angle")),
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
        setAngle(coreData.angle);
        setTextures(coreData.mainTexture, coreData.secondaryTexture);
        setOverlay(coreData.overlay);
        setFlip(coreData.flip);
    }

    public void setAngle(Angle angle) {
        coreData.angle = angle;
        regenerateTransformedBox();
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

    public boolean hasThePermissionToEdit(WithOwner tile, @Nullable PlayerEntity player) {
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
                    setAngle(coreData.angle.add(Angle.fromDegrees(angleToPost.radians() < 0 ? 15 : -15)));
                    notifyAngleChanged(info);
                }
            } else if(!isBrush(heldItem))
                tryTeleport((ServerPlayerEntity) info.player, info.getTilePartInfo());
        } else if(isBrush(heldItem))
            paint(info);
        return InteractionResult.Accepted;
    }

    private void tryTeleport(ServerPlayerEntity player, PostTile.TilePartInfo tilePartInfo) {
        if(Config.Server.teleport.enableTeleport.get() && coreData.destination.isPresent() && (!(coreData.destination.get() instanceof WaystoneHandle.Vanilla) || WaystoneLibrary.getInstance().contains((WaystoneHandle.Vanilla) coreData.destination.get()))) {
            WaystoneHandle dest = coreData.destination.get();
            Optional<WaystoneHandle.Vanilla> vanillaHandle = dest instanceof WaystoneHandle.Vanilla ? Optional.of((WaystoneHandle.Vanilla) dest) : Optional.empty();
            PacketHandler.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Teleport.RequestGui.Package(
                    Either.rightIfPresent(vanillaHandle, () -> ((ExternalWaystone.Handle) dest).noTeleportLangKey())
                        .mapRight(h -> {
                            WaystoneData data = WaystoneLibrary.getInstance().getData(h);
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
                        }),
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
        CompoundNBT compound = new CompoundNBT();
        compound.put("Angle", Angle.Serializer.write(coreData.angle));
        info.mutationDistributor.accept(compound);
    }

    protected void notifyTextureChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        compound.putString("Texture", coreData.mainTexture.toString());
        compound.putString("TextureDark", coreData.secondaryTexture.toString());
        info.mutationDistributor.accept(compound);
    }

    protected void notifyFlipChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        compound.putBoolean("Flip", coreData.flip);
        info.mutationDistributor.accept(compound);
    }

    @Override
    public void readMutationUpdate(CompoundNBT compound, TileEntity tile, PlayerEntity editingPlayer) {
        if(compound.contains("CoreData")) compound = compound.getCompound("CoreData");
        if(compound.contains("Angle"))
            setAngle(Angle.Serializer.read(compound.getCompound("Angle")));

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
            CompoundNBT dest = compound.getCompound("Destination");
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

    private void dropOn(World world, BlockPos pos) {
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

    public Angle getAngle() {
        return coreData.angle;
    }

    public abstract Self copy();

}