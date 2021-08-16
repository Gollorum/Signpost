package gollorum.signpost.blockpartdata.types;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.Post;
import gollorum.signpost.minecraft.utils.CoordinatesUtil;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.AABB;
import gollorum.signpost.utils.math.geometry.Matrix4x4;
import gollorum.signpost.utils.math.geometry.TransformedBox;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Optional;

public class LargeSign extends Sign<LargeSign> {

    private static final AABB LOCAL_BOUNDS = new AABB(
        new Vector3(-9, -14, 2),
        new Vector3(12, -2, 3)
    ).map(CoordinatesUtil::voxelToLocal);

    public static final BlockPartMetadata<LargeSign> METADATA = new BlockPartMetadata<>(
        "large_sign",
        (sign, compound) -> {
            compound.put("CoreData", CoreData.SERIALIZER.write(sign.coreData));
            compound.putString("Text0", sign.text[0]);
            compound.putString("Text1", sign.text[1]);
            compound.putString("Text2", sign.text[2]);
            compound.putString("Text3", sign.text[3]);
        },
        (compound) -> new LargeSign(
            CoreData.SERIALIZER.read(compound.getCompound("CoreData")),
            new String[]{
                compound.getString("Text0"),
                compound.getString("Text1"),
                compound.getString("Text2"),
                compound.getString("Text3")}
        ), LargeSign.class);

    private String[] text;

    public LargeSign(
        CoreData coreData,
        String[] text
    ) {
        super(coreData);
        assert text.length == 4;
        this.text = text;
    }

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
        Post.ModelType modelType,
        boolean isLocked
    ) { this(
        new CoreData(angle, flip, mainTexture, secondaryTexture, overlay,
            color, destination, modelType, itemToDropOnBreak, isLocked),
        text
    ); }

    public void setText(String[] text) {
        this.text = text;
    }

    public String[] getText() { return text; }

    @Override
    protected void regenerateTransformedBox() {
        transformedBounds = new TransformedBox(LOCAL_BOUNDS).rotateAlong(Matrix4x4.Axis.Y, coreData.angle);
        if(coreData.flip) transformedBounds = transformedBounds.scale(new Vector3(1, 1, -1));
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
    public void readMutationUpdate(CompoundNBT compound, TileEntity tile, PlayerEntity editingPlayer) {
        if(editingPlayer != null
            && editingPlayer.isServerWorld()
            && tile instanceof WithOwner.OfSignpost
            && !hasThePermissionToEdit(((WithOwner.OfSignpost)tile), editingPlayer)
        ) {
            // This should not happen unless a player tries to hacc
            editingPlayer.sendMessage(new TranslationTextComponent(LangKeys.noPermissionSignpost), Util.DUMMY_UUID);
            return;
        }
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
        super.readMutationUpdate(compound, tile, editingPlayer);
    }

    @Override
    public LargeSign copy() {
        return new LargeSign(coreData.copy(), text);
    }

    @Override
    public BlockPartMetadata<LargeSign> getMeta() {
        return METADATA;
    }

    @Override
    public void writeTo(CompoundNBT compound) {
        METADATA.write(this, compound);
    }

}
