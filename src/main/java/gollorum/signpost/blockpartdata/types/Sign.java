package gollorum.signpost.blockpartdata.types;

import gollorum.signpost.*;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.block.Post;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.RequestSignGui;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.utils.BlockPart;
import gollorum.signpost.utils.OwnershipData;
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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

public abstract class Sign<Self extends Sign<Self>> implements BlockPart<Self> {

    protected static final class CoreData {
        public Angle angle;
        public boolean flip;
        public ResourceLocation mainTexture;
        public ResourceLocation secondaryTexture;
        public Optional<Overlay> overlay;
        public int color;
        public Optional<WaystoneHandle> destination;
        public Post.ModelType modelType;
        public ItemStack itemToDropOnBreak;
        public OwnershipData ownership;

        public CoreData(
            Angle angle,
            boolean flip,
            ResourceLocation mainTexture,
            ResourceLocation secondaryTexture,
            Optional<Overlay> overlay,
            int color,
            Optional<WaystoneHandle> destination,
            Post.ModelType modelType,
            ItemStack itemToDropOnBreak,
            OwnershipData ownership
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
            this.ownership = ownership;
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
                compound.put("Destination", WaystoneHandle.Serializer.optional().write(coreData.destination));
                compound.put("ItemToDropOnBreak", ItemStackSerializer.Instance.write(coreData.itemToDropOnBreak));
                compound.putString("ModelType", coreData.modelType.name);
                compound.put("Owner", OwnershipData.Serializer.write(coreData.ownership));
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
                    && compound.contains("Owner");
            }

            @Override
            public CoreData read(CompoundNBT compound) {
                return new CoreData(
                    Angle.Serializer.read(compound.getCompound("Angle")),
                    compound.getBoolean("Flip"),
                    new ResourceLocation(compound.getString("Texture")),
                    new ResourceLocation(compound.getString("TextureDark")),
                    Overlay.Serializer.optional().read(compound.getCompound("Overlay")),
                    compound.getInt("Color"),
                    WaystoneHandle.Serializer.optional().read(compound.getCompound("Destination")),
                    Post.ModelType.getByName(compound.getString("ModelType"), true).orElse(Post.ModelType.Oak),
                    ItemStackSerializer.Instance.read(compound.getCompound("ItemToDropOnBreak")),
                    OwnershipData.Serializer.read(compound.getCompound("Owner"))
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

    protected Sign(CoreData coreData) {
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

    private void setModelType(Post.ModelType modelType) {
        coreData.modelType = modelType;
    }

    public ItemStack getItemToDropOnBreak() { return coreData.itemToDropOnBreak; }

    public boolean isFlipped() { return coreData.flip; }

    public int getColor() { return coreData.color; }

    public Post.ModelType getModelType() { return coreData.modelType; }

    public OwnershipData getOwnership() { return coreData.ownership; }

    public boolean hasThePermissionToEdit(@Nullable PlayerEntity player) {
        return !coreData.ownership.isLocked || player == null
            || coreData.ownership.owner.id.equals(player.getUniqueID())
            || player.hasPermissionLevel(Config.Server.permissions.editLockedSignCommandPermissionLevel.get());
    }

    private void setTextures(ResourceLocation texture, ResourceLocation textureDark) {
        coreData.mainTexture = texture;
        coreData.secondaryTexture = textureDark;
    }

    public ResourceLocation getMainTexture() { return coreData.mainTexture; }
    public ResourceLocation getSecondaryTexture() { return coreData.secondaryTexture; }

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
        if (!info.isRemote) {
            if(holdsAngleTool(info)) {
                if(info.player.isSneaking()) {
                    setFlip(!isFlipped());
                    notifyFlipChanged(info);
                } else {
                    Vector3 diff = info.traceResult.ray.start.negated().add(0.5f, 0.5f, 0.5f).withY(0).normalized();
                    Vector3 rayDir = info.traceResult.ray.dir.withY(0).normalized();
                    Angle angleToPost = Angle.between(rayDir.x, rayDir.z, diff.x, diff.z).normalized();
                    setAngle(coreData.angle.add(Angle.fromDegrees(angleToPost.radians() < 0 ? 15 : -15)));
                    notifyAngleChanged(info);
                }
            } else tryTeleport((ServerPlayerEntity) info.player, info.getTilePartInfo());
        }
        return InteractionResult.Accepted;
    }

    private void tryTeleport(ServerPlayerEntity player, PostTile.TilePartInfo tilePartInfo) {
        if(Config.Server.teleport.enableTeleport.get() && coreData.destination.isPresent() && WaystoneLibrary.getInstance().contains(coreData.destination.get())) {
            WaystoneData data = WaystoneLibrary.getInstance().getData(coreData.destination.get());
            boolean isDiscovered = WaystoneLibrary.getInstance()
                .isDiscovered(new PlayerHandle(player), coreData.destination.get()) || !Config.Server.teleport.enforceDiscovery.get();
            int distance = (int) data.location.spawn.distanceTo(Vector3.fromVec3d(player.getPositionVec()));
            PacketHandler.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Teleport.Request.Package(
                    WaystoneLibrary.getInstance().getData(coreData.destination.get()).name,
                    isDiscovered,
                    distance,
                    Config.Server.teleport.maximumDistance.get(),
                    Teleport.getCost(player, Vector3.fromBlockPos(data.location.block.blockPos), data.location.spawn),
                    Optional.of(tilePartInfo)
                )
            );
        } else {
            PacketHandler.send(PacketDistributor.PLAYER.with(() -> player), new RequestSignGui.Package(tilePartInfo));
        }
    }

    private boolean holdsAngleTool(InteractionInfo info) {
        ItemStack itemStack = info.player.getHeldItem(info.hand);
        return !itemStack.isEmpty() && PostTile.isAngleTool(itemStack.getItem());
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
        OptionalSerializer<WaystoneHandle> destinationSerializer = WaystoneHandle.Serializer.optional();
        if(compound.contains("Destination"))
            setDestination(destinationSerializer.read(compound.getCompound("Destination")));
        if(compound.contains("ItemToDropOnBreak")) {
            setItemToDropOnBreak(ItemStackSerializer.Instance.read(compound.getCompound("ItemToDropOnBreak")));
        }
        if(compound.contains("ModelType"))
            Post.ModelType.getByName(compound.getString("ModelType"), true).ifPresent(this::setModelType);

        OptionalSerializer<Overlay> overlaySerializer = Overlay.Serializer.optional();
        if(compound.contains("Overlay"))
            setOverlay(overlaySerializer.read(compound.getCompound("Overlay")));

        if(compound.contains("Owner") && OwnershipData.Serializer.isContainedIn(compound.getCompound("Owner"))) {
            OwnershipData newOwnership = OwnershipData.Serializer.read(compound.getCompound("Owner"));
            if(editingPlayer == null || !editingPlayer.isServerWorld()
                || editingPlayer.getUniqueID().equals(coreData.ownership.owner.id)
                || editingPlayer.hasPermissionLevel(Config.Server.permissions.editLockedSignCommandPermissionLevel.get()))
            coreData.ownership = newOwnership;
        }
        tile.markDirty();
    }

    @Override
    public Collection<ItemStack> getDrops(PostTile tile) {
        return Collections.singleton(coreData.itemToDropOnBreak);
    }

    private void dropOn(World world, BlockPos pos) {
        if(!coreData.itemToDropOnBreak.isEmpty() && !world.isRemote) {
            ItemEntity itementity = new ItemEntity(
                world,
                pos.getX() + world.rand.nextFloat() * 0.5 + 0.25,
                pos.getY() + world.rand.nextFloat() * 0.5 + 0.25,
                pos.getZ() + world.rand.nextFloat() * 0.5 + 0.25,
                coreData.itemToDropOnBreak
            );
            itementity.setDefaultPickupDelay();
            world.addEntity(itementity);
        }
    }

    public Angle getAngle() {
        return coreData.angle;
    }

}
