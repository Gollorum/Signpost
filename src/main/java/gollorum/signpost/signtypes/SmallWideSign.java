package gollorum.signpost.signtypes;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.AABB;
import gollorum.signpost.utils.math.geometry.Matrix4x4;
import gollorum.signpost.utils.math.geometry.TransformedBox;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.OptionalSerializer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Quaternion;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static gollorum.signpost.minecraft.rendering.RenderingUtil.FontToVoxelSize;
import static gollorum.signpost.minecraft.rendering.RenderingUtil.VoxelSize;

public class SmallWideSign extends Sign<SmallWideSign> {

    private static final AABB LOCAL_BOUNDS = new AABB(
        new Vector3(-9, -11, -2),
        new Vector3(16, -5, -3)
    ).map(RenderingUtil::voxelToLocal);

    private static final float TEXT_OFFSET_RIGHT = 7f * VoxelSize;
    private static final float TEXT_OFFSET_LEFT = 11f * VoxelSize;
    private static final float MAXIMUM_TEXT_WIDTH = TEXT_OFFSET_RIGHT + TEXT_OFFSET_LEFT;

    private static final float TEXT_RATIO = 1.3f;
    private static final float FONT_SIZE_VOXELS = 2 / TEXT_RATIO;

    public static final BlockPartMetadata<SmallWideSign> METADATA = new BlockPartMetadata<>(
        "small_wide_sign",
        (sign, keyPrefix, compound) -> {
            Angle.SERIALIZER.writeTo(sign.angle, compound, keyPrefix);
            compound.putString(keyPrefix + "Text", sign.text);
            compound.putBoolean(keyPrefix + "Flip", sign.flip);
            compound.putString(keyPrefix + "Texture", sign.mainTexture.toString());
            compound.putString(keyPrefix + "TextureDark", sign.secondaryTexture.toString());
            compound.putInt(keyPrefix + "Color", sign.color);
            OptionalSerializer.UUID.writeTo(sign.destination, compound, "Destination");
        },
        (compound, keyPrefix) -> new SmallWideSign(
            Angle.SERIALIZER.read(compound, keyPrefix),
            compound.getString(keyPrefix + "Text"),
            compound.getBoolean(keyPrefix + "Flip"),
            new ResourceLocation(compound.getString(keyPrefix + "Texture")),
            new ResourceLocation(compound.getString(keyPrefix + "TextureDark")),
            compound.getInt(keyPrefix + "Color"),
            OptionalSerializer.UUID.read(compound, "Destination")
        )
    );

    private String text;

    public SmallWideSign(Angle angle, String text, boolean flip, ResourceLocation mainTexture, ResourceLocation secondaryTexture, int color, Optional<UUID> destination){
        super(angle, flip, mainTexture, secondaryTexture, color, destination);
        this.text = text;
    }

    public void setText(String text) { this.text = text; }

    @Override
    protected ResourceLocation getModel() {
        return RenderingUtil.ModelShortSign;
    }

    @Override
    protected void regenerateTransformedBox() {
        transformedBounds = new TransformedBox(LOCAL_BOUNDS).rotateAlong(Matrix4x4.Axis.Y, angle);
        if(flip) transformedBounds = transformedBounds.scale(new Vector3(-1, 1, 1));
    }

    private void notifyTextChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        compound.putString("type", "text");
        compound.putString("text", text);
        info.mutationDistributor.accept(compound);
    }

    @Override
    public void readMutationUpdate(CompoundNBT compound, TileEntity tile) {
        if (compound.getString("type").equals("text")) {
            text = compound.getString("text");
            return;
        }
        super.readMutationUpdate(compound, tile);
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
            float scale = FONT_SIZE_VOXELS * FontToVoxelSize;
            float MAX_WIDTH_FRAC = fontRenderer.getStringWidth(text) * scale / MAXIMUM_TEXT_WIDTH;
            scale /= Math.max(1, MAX_WIDTH_FRAC);
            matrix.rotate(Vector3f.YP.rotation(-angle.radians()));
            float offset = TEXT_OFFSET_RIGHT * Math.min(1, MAX_WIDTH_FRAC);
            matrix.translate(
                flip ? -offset : offset - fontRenderer.getStringWidth(text) * scale,
                -scale * 4 * TEXT_RATIO,
                -3.005 * VoxelSize);
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
