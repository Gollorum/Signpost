package gollorum.signpost.signtypes;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.Post;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.MathUtils;
import gollorum.signpost.utils.math.geometry.AABB;
import gollorum.signpost.utils.math.geometry.Matrix4x4;
import gollorum.signpost.utils.math.geometry.TransformedBox;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.OptionalSerializer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static gollorum.signpost.minecraft.rendering.RenderingUtil.FontToVoxelSize;
import static gollorum.signpost.minecraft.rendering.RenderingUtil.VoxelSize;

public class SmallShortSign extends Sign<SmallShortSign> {

    private static final AABB LOCAL_BOUNDS = new AABB(
        new Vector3(2, -11, 0.5f),
        new Vector3(18, -5, -0.5f)
    ).map(RenderingUtil::voxelToLocal);

    private static final float TEXT_OFFSET_RIGHT = -3f * VoxelSize;
    private static final float TEXT_OFFSET_LEFT = 13.5f * VoxelSize;
    private static final float MAXIMUM_TEXT_WIDTH = TEXT_OFFSET_RIGHT + TEXT_OFFSET_LEFT;

    private static final float TEXT_RATIO = 1.3f;
    private static final float FONT_SIZE_VOXELS = 2 / TEXT_RATIO;

    public static final BlockPartMetadata<SmallShortSign> METADATA = new BlockPartMetadata<>(
        "small_short_sign",
        (sign, keyPrefix, compound) -> {
            Angle.SERIALIZER.writeTo(sign.angle, compound, keyPrefix + "Angle");
            compound.putString(keyPrefix + "Text", sign.text);
            compound.putBoolean(keyPrefix + "Flip", sign.flip);
            compound.putString(keyPrefix + "Texture", sign.mainTexture.toString());
            compound.putString(keyPrefix + "TextureDark", sign.secondaryTexture.toString());
            compound.putInt(keyPrefix + "Color", sign.color);
            new OptionalSerializer(WaystoneHandle.SERIALIZER).writeTo(sign.destination, compound, "Destination");
            OptionalSerializer.ItemStack.writeTo(sign.itemToDropOnBreak, compound, "ItemToDropOnBreak");
            compound.putString(keyPrefix + "ModelType", sign.modelType.name());
        },
        (compound, keyPrefix) -> new SmallShortSign(
            Angle.SERIALIZER.read(compound, keyPrefix + "Angle"),
            compound.getString(keyPrefix + "Text"),
            compound.getBoolean(keyPrefix + "Flip"),
            new ResourceLocation(compound.getString(keyPrefix + "Texture")),
            new ResourceLocation(compound.getString(keyPrefix + "TextureDark")),
            compound.getInt(keyPrefix + "Color"),
            new OptionalSerializer(WaystoneHandle.SERIALIZER).read(compound, "Destination"),
            OptionalSerializer.ItemStack.read(compound, "ItemToDropOnBreak"),
            Post.ModelType.valueOf(compound.getString(keyPrefix + "ModelType"))
        )
    );

    private String text;

    public SmallShortSign(
        Angle angle,
        String text,
        boolean flip,
        ResourceLocation mainTexture,
        ResourceLocation secondaryTexture,
        int color,
        Optional<WaystoneHandle> destination,
        Optional<ItemStack> itemToDropOnBreak,
        Post.ModelType modelType
    ){
        super(angle, flip, mainTexture, secondaryTexture, color, destination, modelType, itemToDropOnBreak);
        this.text = text;
    }

    public void setText(String text) { this.text = text; }

    public String getText() { return text; }

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
        compound.putString("Text", text);
        info.mutationDistributor.accept(compound);
    }

    @Override
    public void readMutationUpdate(CompoundNBT compound, TileEntity tile) {
        if (compound.contains("Text")) {
            setText(compound.getString("Text"));
        }
        super.readMutationUpdate(compound, tile);
    }

    @Override
    public void render(TileEntity tileEntity, TileEntityRendererDispatcher renderDispatcher, MatrixStack matrix, IRenderTypeBuffer buffer, int combinedLights, int combinedOverlay, Random random, long randomSeed) {
        RenderingUtil.render(matrix, renderModel -> {
            matrix.push();
            matrix.rotate(new Quaternion(Vector3f.YP, angle.radians(), false));
            Matrix4f rotationMatrix = new Matrix4f(new Quaternion(Vector3f.YP, angle.radians(), false));
            if (flip) {
                matrix.rotate(Vector3f.ZP.rotationDegrees(180));
                rotationMatrix.mul(Vector3f.ZP.rotationDegrees(180));
            }
            renderModel.render(
                withTransformedDirections(model.get(), flip, angle.degrees()),
                tileEntity,
                buffer.getBuffer(RenderType.getSolid()),
                false,
                random,
                randomSeed,
                combinedOverlay,
                rotationMatrix
            );
            matrix.pop();
            matrix.rotate(Vector3f.ZP.rotationDegrees(180));
            FontRenderer fontRenderer = renderDispatcher.fontRenderer;
            float scale = FONT_SIZE_VOXELS * FontToVoxelSize;
            float MAX_WIDTH_FRAC = fontRenderer.getStringWidth(text) * scale / MAXIMUM_TEXT_WIDTH;
            scale /= Math.max(1, MAX_WIDTH_FRAC);
            matrix.rotate(Vector3f.YP.rotation((float) (Math.PI-angle.radians())));
            float offset = MathUtils.lerp(TEXT_OFFSET_RIGHT, (TEXT_OFFSET_RIGHT - TEXT_OFFSET_LEFT) / 2f, 1 - Math.min(1, MAX_WIDTH_FRAC));
            matrix.translate(
                flip ? offset - fontRenderer.getStringWidth(text) * scale : -offset,
                -scale * 4 * TEXT_RATIO,
                -0.505 * VoxelSize);
            matrix.scale(scale, scale * TEXT_RATIO, scale);
            fontRenderer.renderString(text, 0, 0, color, false, matrix.getLast().getMatrix(), buffer, false, 0, combinedLights);
        });
    }

    @Override
    public BlockPartMetadata<SmallShortSign> getMeta() {
        return METADATA;
    }

    @Override
    public void writeTo(CompoundNBT compound, String keyPrefix) {
        METADATA.writeTo(this, compound, keyPrefix);
    }

}
