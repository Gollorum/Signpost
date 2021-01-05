package gollorum.signpost.signdata.types;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.*;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.Post;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.LangKeys;
import gollorum.signpost.minecraft.gui.SignGui;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.signdata.Overlay;
import gollorum.signpost.utils.BlockPart;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Intersectable;
import gollorum.signpost.utils.math.geometry.Ray;
import gollorum.signpost.utils.math.geometry.TransformedBox;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.ItemStackSerializer;
import gollorum.signpost.utils.serialization.OptionalSerializer;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Lazy;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static gollorum.signpost.minecraft.rendering.RenderingUtil.withTintIndex;

public abstract class Sign<Self extends Sign<Self>> implements BlockPart<Self> {

    protected Angle angle;
    protected int color;
    protected boolean flip;
    protected ResourceLocation mainTexture;
    protected ResourceLocation secondaryTexture;
    protected Optional<Overlay> overlay;
    protected Optional<WaystoneHandle> destination;
    protected Post.ModelType modelType;
    public Optional<WaystoneHandle> getDestination() { return destination; }

    protected TransformedBox transformedBounds;
    protected Lazy<IBakedModel> model;
    protected Optional<Lazy<IBakedModel>> overlayModel;

    public ItemStack itemToDropOnBreak;

    public Sign(Angle angle, boolean flip, ResourceLocation mainTexture, ResourceLocation secondaryTexture, Optional<Overlay> overlay, int color, Optional<WaystoneHandle> destination, Post.ModelType modelType, ItemStack itemToDropOnBreak) {
        this.color = color;
        this.destination = destination;
        this.modelType = modelType;
        this.itemToDropOnBreak = itemToDropOnBreak;
        setAngle(angle);
        setTextures(mainTexture, secondaryTexture);
        setOverlay(overlay);
        setFlip(flip);
    }

    public void setAngle(Angle angle) {
        this.angle = angle;
        regenerateTransformedBox();
    }

    public void setFlip(boolean flip) {
        this.flip = flip;
        setTextures(mainTexture, secondaryTexture);
        setOverlay(overlay);
        regenerateTransformedBox();
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setDestination(Optional<WaystoneHandle> destination) {
        this.destination = destination;
    }

    public void setItemToDropOnBreak(ItemStack itemToDropOnBreak) {
        this.itemToDropOnBreak = itemToDropOnBreak;
    }

    private void setModelType(Post.ModelType modelType) {
        this.modelType = modelType;
    }

    public boolean isFlipped() { return flip; }

    protected abstract Lazy<IBakedModel> makeModel();
    protected abstract Lazy<IBakedModel> makeOverlayModel(Overlay texture);

    public int getColor() { return color; }

    private void setTextures(ResourceLocation texture, ResourceLocation textureDark) {
        this.mainTexture = texture;
        this.secondaryTexture = textureDark;
        model = makeModel();
    }

    public ResourceLocation getMainTexture() { return mainTexture; }
    public ResourceLocation getSecondaryTexture() { return secondaryTexture; }

    private void setOverlay(Optional<Overlay> overlay) {
        this.overlay = overlay;
        overlayModel = overlay.map(this::makeOverlayModel);
    }

    public Optional<Overlay> getOverlay() { return overlay; }

    protected abstract void regenerateTransformedBox();

    @Override
    public Intersectable<Ray, Float> getIntersection() {
        return transformedBounds;
    }

    @Override
    public InteractionResult interact(InteractionInfo info) {
        if (!info.isRemote) {
            if(holdsAngleTool(info)) {
                Vector3 diff = info.traceResult.ray.start.negated().add(0.5f, 0.5f, 0.5f).withY(0).normalized();
                Vector3 rayDir = info.traceResult.ray.dir.withY(0).normalized();
                Angle angleToPost = Angle.between(rayDir.x, rayDir.z, diff.x, diff.z).normalized();
                setAngle(angle.add(Angle.fromDegrees(angleToPost.radians() < 0 ? 15 : -15)));
                notifyAngleChanged(info);
            } else if(!holdsEditTool(info))
                tryTeleport((ServerPlayerEntity) info.player);
        } else if(holdsEditTool(info)) {
            Minecraft.getInstance().displayGuiScreen(
                new SignGui(info.tile, modelType, this, new PostTile.TilePartInfo(info.tile, info.traceResult.id)));
        }
        return InteractionResult.Accepted;
    }

    private void tryTeleport(ServerPlayerEntity player) {
        if(destination.isPresent() && WaystoneLibrary.getInstance().contains(destination.get()))
            if(WaystoneLibrary.getInstance().isDiscovered(new PlayerHandle(player), destination.get()))
                Teleport.toWaystone(destination.get(), player);
            else player.sendMessage(
                new TranslationTextComponent(LangKeys.notDiscovered, WaystoneLibrary.getInstance().getData(destination.get()).name),
                Util.DUMMY_UUID);
        else {
            player.sendMessage(new TranslationTextComponent(LangKeys.noTeleport), Util.DUMMY_UUID);
        }
    }

    private boolean holdsAngleTool(InteractionInfo info) {
        ItemStack itemStack = info.player.getHeldItem(info.hand);
        return !itemStack.isEmpty() && PostTile.isAngleTool(itemStack.getItem());
    }

    private boolean holdsEditTool(InteractionInfo info) {
        ItemStack itemStack = info.player.getHeldItem(info.hand);
        return !itemStack.isEmpty() && PostTile.isEditTool(itemStack.getItem());
    }

    protected void notifyAngleChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        Angle.SERIALIZER.writeTo(angle, compound, "Angle");
        info.mutationDistributor.accept(compound);
    }

    protected void notifyTextureChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        compound.putString("Texture", mainTexture.toString());
        compound.putString("TextureDark", secondaryTexture.toString());
        info.mutationDistributor.accept(compound);
    }

    protected void notifyFlipChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        compound.putBoolean("Flip", flip);
        info.mutationDistributor.accept(compound);
    }

    @Override
    public void readMutationUpdate(CompoundNBT compound, TileEntity tile) {
        if(Angle.SERIALIZER.isContainedIn(compound, "Angle"))
            setAngle(Angle.SERIALIZER.read(compound, "Angle"));

        boolean updateTextures = false;
        if(compound.contains("Texture")) {
            mainTexture = new ResourceLocation(compound.getString("Texture"));
            updateTextures = true;
        }
        if(compound.contains("TextureDark")){
            secondaryTexture = new ResourceLocation(compound.getString("TextureDark"));
            updateTextures = true;
        }
        if(updateTextures) setTextures(mainTexture, secondaryTexture);

        if(compound.contains("Flip")) setFlip(compound.getBoolean("Flip"));
        if(compound.contains("Color")) setColor(compound.getInt("Color"));
        OptionalSerializer<WaystoneHandle> destinationSerializer = new OptionalSerializer<>(WaystoneHandle.SERIALIZER);
        if(destinationSerializer.isContainedIn(compound, "Destination"))
            setDestination(destinationSerializer.read(compound, "Destination"));
        if(ItemStackSerializer.Instance.isContainedIn(compound, "ItemToDropOnBreak")) {
            setItemToDropOnBreak(ItemStackSerializer.Instance.read(compound, "ItemToDropOnBreak"));
        }
        if(compound.contains("ModelType"))
            setModelType(Post.ModelType.valueOf(compound.getString("ModelType")));

        OptionalSerializer<Overlay> overlaySerializer = new OptionalSerializer<>(Overlay.Serializer);
        if(overlaySerializer.isContainedIn(compound, "Overlay"))
            setOverlay(overlaySerializer.read(compound, "Overlay"));
        tile.markDirty();
    }

    @Override
    public Collection<ItemStack> getDrops(PostTile tile) {
        return Collections.singleton(itemToDropOnBreak);
    }

    private void dropOn(World world, BlockPos pos) {
        if(!itemToDropOnBreak.isEmpty() && !world.isRemote) {
            ItemEntity itementity = new ItemEntity(
                world,
                pos.getX() + world.rand.nextFloat() * 0.5 + 0.25,
                pos.getY() + world.rand.nextFloat() * 0.5 + 0.25,
                pos.getZ() + world.rand.nextFloat() * 0.5 + 0.25,
                itemToDropOnBreak
            );
            itementity.setDefaultPickupDelay();
            world.addEntity(itementity);
        }
    }

    @Override
    public void render(TileEntity tileEntity, TileEntityRendererDispatcher renderDispatcher, MatrixStack matrix, IRenderTypeBuffer buffer, int combinedLights, int combinedOverlay, Random random, long randomSeed) {
        RenderingUtil.render(matrix, renderModel -> {
            matrix.push();
            Quaternion rotation = new Quaternion(Vector3f.YP, angle.radians(), false);
            matrix.rotate(rotation);
            Matrix4f rotationMatrix = new Matrix4f(rotation);
            renderModel.render(
                model.get(),
                tileEntity,
                buffer.getBuffer(RenderType.getSolid()),
                false,
                random,
                randomSeed,
                combinedOverlay,
                rotationMatrix
            );
            overlayModel.ifPresent(m -> {
                renderModel.render(
                    withTintIndex(m.get(), overlay.map(o -> o.tintIndex).orElse(0)),
                    tileEntity,
                    buffer.getBuffer(RenderType.getCutoutMipped()),
                    true,
                    random,
                    randomSeed,
                    combinedOverlay,
                    rotationMatrix
                );
            });
            matrix.pop();
            renderText(matrix, renderDispatcher.getFontRenderer(), buffer, combinedLights);
        });
    }

    protected abstract void renderText(MatrixStack matrix, FontRenderer fontRenderer, IRenderTypeBuffer buffer, int combinedLights);

    public Angle getAngle() {
        return angle;
    }
}
