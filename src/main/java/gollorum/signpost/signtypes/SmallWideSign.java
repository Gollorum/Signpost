package gollorum.signpost.signtypes;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.BlockPart;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.*;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Quaternion;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Lazy;

import java.util.Random;

import static gollorum.signpost.minecraft.rendering.RenderingUtil.FONT_TO_VOXEL_SIZE;
import static gollorum.signpost.minecraft.rendering.RenderingUtil.VOXEL_SIZE;

public class SmallWideSign implements BlockPart<SmallWideSign> {

    private static final AABB LOCAL_BOUNDS = new AABB(
        new Vector3(-9, -11, -2),
        new Vector3(16, -5, -3)
    ).map(RenderingUtil::voxelToLocal);

    private static final float TEXT_OFFSET_RIGHT = 7f * VOXEL_SIZE;
    private static final float TEXT_OFFSET_LEFT = 11f * VOXEL_SIZE;
    private static final float MAXIMUM_TEXT_WIDTH = TEXT_OFFSET_RIGHT + TEXT_OFFSET_LEFT;

    private static final float TEXT_RATIO = 1.3f;
    private static final float FONT_SIZE_VOXELS = 2 / TEXT_RATIO;

    public static final BlockPartMetadata<SmallWideSign> METADATA = new BlockPartMetadata<>(
        "small_wide_sign",
        (sign, keyPrefix, compound) -> {
            Angle.SERIALIZER.writeTo(sign.angle, compound, keyPrefix);
            compound.putString(keyPrefix + "text", sign.text);
            compound.putBoolean(keyPrefix + "flip", sign.flip);
            compound.putString(keyPrefix + "texture", sign.texture.toString());
        },
        (compound, keyPrefix) -> new SmallWideSign(
            Angle.SERIALIZER.read(compound, keyPrefix),
            compound.getString(keyPrefix + "text"),
            compound.getBoolean(keyPrefix + "flip"),
            new ResourceLocation(compound.getString(keyPrefix + "texture"))
        )
    );

    private Angle angle;
    private String text;
    private boolean flip;
    private ResourceLocation texture;

    private TransformedBox transformedBounds;
    private Lazy<IBakedModel> model;

    public SmallWideSign(Angle angle, String text, boolean flip, ResourceLocation texture){
        setAngle(angle);
        this.text = text;
        setTexture(texture);
        setFlip(flip);
    }

    public void setAngle(Angle angle) {
        this.angle = angle;
        regenerateTransformedBox();
    }

    public void setFlip(boolean flip) {
        this.flip = flip;
        regenerateTransformedBox();
    }

    public void setTexture(ResourceLocation texture){
        model = RenderingUtil.loadModel(RenderingUtil.MODEL_SIGN, texture);
        this.texture = texture;
    }

    public void setText(String text) { this.text = text; }

    private void regenerateTransformedBox() {
        transformedBounds = new TransformedBox(LOCAL_BOUNDS).rotateAlong(Matrix4x4.Axis.Y, angle);
        if(flip) transformedBounds = transformedBounds.scale(new Vector3(-1, 1, 1));
    }

    @Override
    public Intersectable<Ray, Float> getIntersection() {
        return transformedBounds;
    }

    @Override
    public InteractionResult interact(InteractionInfo info) {
        // TODO Implement.
        setAngle(angle.add(Angle.fromDegrees(15)));
        notifyAngleChanged(info);
        return InteractionResult.Accepted;
    }

    private void notifyAngleChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        compound.putString("type", "angle");
        compound.putFloat("angle_radians", angle.radians());
        info.mutationDistributor.accept(compound);
    }

    private void notifyTextureChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        compound.putString("type", "texture");
        compound.putString("texture", texture.toString());
        info.mutationDistributor.accept(compound);
    }

    private void notifyTextChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        compound.putString("type", "text");
        compound.putString("text", text);
        info.mutationDistributor.accept(compound);
    }

    private void notifyFlipChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        compound.putString("type", "flip");
        compound.putBoolean("flip", flip);
        info.mutationDistributor.accept(compound);
    }

    @Override
    public void readMutationUpdate(CompoundNBT compound, TileEntity tile) {
        switch (compound.getString("type")){
            case "angle":
                setAngle(Angle.fromRadians(compound.getFloat("angle_radians")));
                break;
            case "texture":
                setTexture(new ResourceLocation(compound.getString("texture")));
                break;
            case "text":
                text = compound.getString("text");
                break;
            case "flip":
                flip = compound.getBoolean("flip");
                break;
        }
    }

    @Override
    public void render(TileEntity tileEntity, TileEntityRendererDispatcher renderDispatcher, MatrixStack matrix, IRenderTypeBuffer buffer, int combinedLights, int combinedOverlay, Random random, long randomSeed) {
        RenderingUtil.render(matrix, renderModel -> {
            matrix.push();
            matrix.rotate(new Quaternion(Vector3f.YP, angle.radians(), false));
            if(flip) matrix.rotate(Vector3f.ZP.rotationDegrees(180));
            renderModel.render(
                model.get(),
                tileEntity,
                buffer.getBuffer(RenderType.getSolid()),
                false,
                random,
                randomSeed,
                combinedOverlay
            );
            matrix.pop();
            matrix.rotate(Vector3f.ZP.rotationDegrees(180));
            FontRenderer fontRenderer = renderDispatcher.fontRenderer;
            float scale = FONT_SIZE_VOXELS * FONT_TO_VOXEL_SIZE;
            float MAX_WIDTH_FRAC = fontRenderer.getStringWidth(text) * scale / MAXIMUM_TEXT_WIDTH;
            scale /= Math.max(1, MAX_WIDTH_FRAC);
            int color = 0;
            matrix.rotate(Vector3f.YP.rotation(-angle.radians()));
            float offset = TEXT_OFFSET_RIGHT * Math.min(1, MAX_WIDTH_FRAC);
            matrix.translate(
                flip ? -offset : offset - fontRenderer.getStringWidth(text) * scale,
                -scale * 4 * TEXT_RATIO,
                -3.001 * VOXEL_SIZE);
            matrix.scale(scale, scale * TEXT_RATIO, scale);
            fontRenderer.renderString(text, 0, 0, color, false, matrix.getLast().getMatrix(), buffer, false, 0, combinedLights);
        });
    }

    @Override
    public BlockPartMetadata<SmallWideSign> getMeta() {
        return METADATA;
    }

    @Override
    public void writeTo(CompoundNBT compound, String keyPrefix) {
        METADATA.writeTo(this, compound, keyPrefix);
    }

}
