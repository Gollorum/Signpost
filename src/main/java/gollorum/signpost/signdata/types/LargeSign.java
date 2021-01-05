package gollorum.signpost.signdata.types;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.Post;
import gollorum.signpost.minecraft.data.PostModel;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.signdata.Overlay;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.AABB;
import gollorum.signpost.utils.math.geometry.Matrix4x4;
import gollorum.signpost.utils.math.geometry.TransformedBox;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.ItemStackSerializer;
import gollorum.signpost.utils.serialization.OptionalSerializer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.common.util.Lazy;

import java.util.Optional;

import static gollorum.signpost.minecraft.rendering.RenderingUtil.FontToVoxelSize;
import static gollorum.signpost.minecraft.rendering.RenderingUtil.VoxelSize;

public class LargeSign extends Sign<LargeSign> {

    private static final AABB LOCAL_BOUNDS = new AABB(
        new Vector3(-9, -14, 2),
        new Vector3(12, -2, 3)
    ).map(RenderingUtil::voxelToLocal);

    private static final float TEXT_OFFSET_RIGHT = 7f * VoxelSize;
    private static final float TEXT_OFFSET_LEFT_SHORT = 9f * VoxelSize;
    private static final float TEXT_OFFSET_LEFT_LONG = 10f * VoxelSize;
    private static final float MAXIMUM_TEXT_WIDTH_SHORT = TEXT_OFFSET_RIGHT + TEXT_OFFSET_LEFT_SHORT;
    private static final float MAXIMUM_TEXT_WIDTH_LONG = TEXT_OFFSET_RIGHT + TEXT_OFFSET_LEFT_LONG;

    private static final float TEXT_RATIO = 1.3f;
    private static final float FONT_SIZE_VOXELS = 2 / TEXT_RATIO;

    public static final BlockPartMetadata<LargeSign> METADATA = new BlockPartMetadata<>(
        "large_sign",
        (sign, keyPrefix, compound) -> {
            Angle.SERIALIZER.writeTo(sign.angle, compound, keyPrefix + "Angle");
            compound.putString(keyPrefix + "Text0", sign.text[0]);
            compound.putString(keyPrefix + "Text1", sign.text[1]);
            compound.putString(keyPrefix + "Text2", sign.text[2]);
            compound.putString(keyPrefix + "Text3", sign.text[3]);
            compound.putBoolean(keyPrefix + "Flip", sign.flip);
            compound.putString(keyPrefix + "Texture", sign.mainTexture.toString());
            compound.putString(keyPrefix + "TextureDark", sign.secondaryTexture.toString());
            new OptionalSerializer<>(Overlay.Serializer).writeTo(sign.overlay, compound, "Overlay");
            compound.putInt(keyPrefix + "Color", sign.color);
            new OptionalSerializer<>(WaystoneHandle.SERIALIZER).writeTo(sign.destination, compound, "Destination");
            ItemStackSerializer.Instance.writeTo(sign.itemToDropOnBreak, compound, "ItemToDropOnBreak");
            compound.putString(keyPrefix + "ModelType", sign.modelType.name());
        },
        (compound, keyPrefix) -> new LargeSign(
            Angle.SERIALIZER.read(compound, keyPrefix + "Angle"),
            new String[]{
                compound.getString(keyPrefix + "Text0"),
                compound.getString(keyPrefix + "Text1"),
                compound.getString(keyPrefix + "Text2"),
                compound.getString(keyPrefix + "Text3")},
            compound.getBoolean(keyPrefix + "Flip"),
            new ResourceLocation(compound.getString(keyPrefix + "Texture")),
            new ResourceLocation(compound.getString(keyPrefix + "TextureDark")),
            new OptionalSerializer<>(Overlay.Serializer).read(compound, "Overlay"),
            compound.getInt(keyPrefix + "Color"),
            new OptionalSerializer<>(WaystoneHandle.SERIALIZER).read(compound, "Destination"),
            ItemStackSerializer.Instance.read(compound, "ItemToDropOnBreak"),
            Post.ModelType.valueOf(compound.getString(keyPrefix + "ModelType"))
        ));

    private String[] text;

    public LargeSign(
        Angle angle,
        String[] text,
        boolean flip,
        ResourceLocation mainTexture,
        ResourceLocation secondaryTexture,
        Optional<Overlay> overlay,
        int color,
        Optional<WaystoneHandle> destination,
        ItemStack itemToDropOnBreak,
        Post.ModelType modelType
    ) {
        super(angle, flip, mainTexture, secondaryTexture, overlay, color, destination, modelType, itemToDropOnBreak);
        assert text.length == 4;
        this.text = text;
    }

    public void setText(String[] text) {
        this.text = text;
    }

    public String[] getText() { return text; }

    @Override
    protected Lazy<IBakedModel> makeModel() {
        return RenderingUtil.loadModel(
            isFlipped() ? PostModel.largeFlippedLocation : PostModel.largeLocation,
            mainTexture, secondaryTexture
        );
    }

    @Override
    protected Lazy<IBakedModel> makeOverlayModel(Overlay texture) {
        return RenderingUtil.loadModel(
            isFlipped() ? PostModel.largeOverlayFlippedLocation : PostModel.largeOverlayLocation,
            texture.textureFor(LargeSign.class)
        );
    }

    @Override
    protected void regenerateTransformedBox() {
        transformedBounds = new TransformedBox(LOCAL_BOUNDS).rotateAlong(Matrix4x4.Axis.Y, angle);
        if(flip) transformedBounds = transformedBounds.scale(new Vector3(1, 1, -1));
    }

    private void notifyTextChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        compound.putString("Text0", text[0]);
        compound.putString("Text1", text[1]);
        compound.putString("Text2", text[2]);
        compound.putString("Text3", text[3]);
        info.mutationDistributor.accept(compound);
    }

    @Override
    public void readMutationUpdate(CompoundNBT compound, TileEntity tile) {
        if (compound.contains("Text0")) {
            text[0] = compound.getString("Text0");
        }
        if (compound.contains("Text1")) {
            text[1] = compound.getString("Text1");
        }
        if (compound.contains("Text2")) {
            text[2] = compound.getString("Text2");
        }
        if (compound.contains("Text3")) {
            text[3] = compound.getString("Text3");
        }
        super.readMutationUpdate(compound, tile);
    }

    @Override
    public void renderText(MatrixStack matrix, FontRenderer fontRenderer, IRenderTypeBuffer buffer, int combinedLights) {
        matrix.rotate(Vector3f.ZP.rotationDegrees(180));
        matrix.rotate(Vector3f.YP.rotation((float) (
            flip
                ? angle.radians()
                : Math.PI - angle.radians())));
        matrix.translate(0, 3.5f * VoxelSize, -3.005 * VoxelSize);

        matrix.push();
        render(fontRenderer, text[3], matrix, buffer, combinedLights, false);
        matrix.pop();
        matrix.translate(0, -7 / 3f * VoxelSize, 0);

        matrix.push();
        render(fontRenderer, text[2], matrix, buffer, combinedLights, true);
        matrix.pop();
        matrix.translate(0, -7 / 3f * VoxelSize, 0);

        matrix.push();
        render(fontRenderer, text[1], matrix, buffer, combinedLights, true);
        matrix.pop();
        matrix.translate(0, -7 / 3f * VoxelSize, 0);

        matrix.push();
        render(fontRenderer, text[0], matrix, buffer, combinedLights, false);
        matrix.pop();
    }

    private void render(FontRenderer fontRenderer, String text, MatrixStack matrix, IRenderTypeBuffer buffer, int combinedLights, boolean isLong) {
        float scale = FONT_SIZE_VOXELS * FontToVoxelSize;
        float MAX_WIDTH_FRAC = fontRenderer.getStringWidth(text) * scale / (isLong ? MAXIMUM_TEXT_WIDTH_LONG : MAXIMUM_TEXT_WIDTH_SHORT);
        scale /= Math.max(1, MAX_WIDTH_FRAC);
        float offset = TEXT_OFFSET_RIGHT * Math.min(1, MAX_WIDTH_FRAC);
        matrix.translate(
            flip ? offset - fontRenderer.getStringWidth(text) * scale : -offset,
            -scale * 4 * TEXT_RATIO,
            0);
        matrix.scale(scale, scale * TEXT_RATIO, scale);
        fontRenderer.renderString(text, 0, 0, color, false, matrix.getLast().getMatrix(), buffer, false, 0, combinedLights);
    }

    @Override
    public BlockPartMetadata<LargeSign> getMeta() {
        return METADATA;
    }

    @Override
    public void writeTo(CompoundNBT compound, String keyPrefix) {
        METADATA.writeTo(this, compound, keyPrefix);
    }

}
