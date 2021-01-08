package gollorum.signpost.signdata.types;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.Post;
import gollorum.signpost.minecraft.utils.CoordinatesUtil;
import gollorum.signpost.signdata.Overlay;
import gollorum.signpost.signdata.types.renderers.ShortSignRenderer;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.AABB;
import gollorum.signpost.utils.math.geometry.Matrix4x4;
import gollorum.signpost.utils.math.geometry.TransformedBox;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.ItemStackSerializer;
import gollorum.signpost.utils.serialization.OptionalSerializer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import java.util.Optional;

public class SmallShortSign extends Sign<SmallShortSign> {

    private static final AABB LOCAL_BOUNDS = new AABB(
        new Vector3(2, -11, 0.5f),
        new Vector3(18, -5, -0.5f)
    ).map(CoordinatesUtil::voxelToLocal);

    public static final BlockPartMetadata<SmallShortSign> METADATA = new BlockPartMetadata<>(
        "small_short_sign",
        (sign, keyPrefix, compound) -> {
            Angle.SERIALIZER.writeTo(sign.angle, compound, keyPrefix + "Angle");
            compound.putString(keyPrefix + "Text", sign.text);
            compound.putBoolean(keyPrefix + "Flip", sign.flip);
            compound.putString(keyPrefix + "Texture", sign.mainTexture.toString());
            compound.putString(keyPrefix + "TextureDark", sign.secondaryTexture.toString());
            new OptionalSerializer<>(Overlay.Serializer).writeTo(sign.overlay, compound, "Overlay");
            compound.putInt(keyPrefix + "Color", sign.color);
            new OptionalSerializer<>(WaystoneHandle.SERIALIZER).writeTo(sign.destination, compound, "Destination");
            ItemStackSerializer.Instance.writeTo(sign.itemToDropOnBreak, compound, "ItemToDropOnBreak");
            compound.putString(keyPrefix + "ModelType", sign.modelType.name());
        },
        (compound, keyPrefix) -> new SmallShortSign(
            Angle.SERIALIZER.read(compound, keyPrefix + "Angle"),
            compound.getString(keyPrefix + "Text"),
            compound.getBoolean(keyPrefix + "Flip"),
            new ResourceLocation(compound.getString(keyPrefix + "Texture")),
            new ResourceLocation(compound.getString(keyPrefix + "TextureDark")),
            new OptionalSerializer<>(Overlay.Serializer).read(compound, "Overlay"),
            compound.getInt(keyPrefix + "Color"),
            new OptionalSerializer<>(WaystoneHandle.SERIALIZER).read(compound, "Destination"),
            ItemStackSerializer.Instance.read(compound, "ItemToDropOnBreak"),
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
        Optional<Overlay> overlay,
        int color,
        Optional<WaystoneHandle> destination,
        ItemStack itemToDropOnBreak,
        Post.ModelType modelType
    ){
        super(angle, flip, mainTexture, secondaryTexture, overlay, color, destination, modelType, itemToDropOnBreak);
        this.text = text;
    }

    public void setText(String text) { this.text = text; }

    public String getText() { return text; }

    @Override
    protected void regenerateTransformedBox() {
        transformedBounds = new TransformedBox(LOCAL_BOUNDS).rotateAlong(Matrix4x4.Axis.Y, angle);
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
    public BlockPartMetadata<SmallShortSign> getMeta() {
        return METADATA;
    }

    @Override
    public void writeTo(CompoundNBT compound, String keyPrefix) {
        METADATA.writeTo(this, compound, keyPrefix);
    }

}
