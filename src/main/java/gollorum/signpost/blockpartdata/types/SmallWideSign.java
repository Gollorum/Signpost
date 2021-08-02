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

public class SmallWideSign extends Sign<SmallWideSign> {

    private static final AABB LOCAL_BOUNDS = new AABB(
        new Vector3(-9, -11, 2),
        new Vector3(16, -5, 3)
    ).map(CoordinatesUtil::voxelToLocal);

    public static final BlockPartMetadata<SmallWideSign> METADATA = new BlockPartMetadata<>(
        "small_wide_sign",
        (sign, compound) -> {
            compound.put("CoreData", CoreData.SERIALIZER.write(sign.coreData));
            compound.putString("Text", sign.text);
        },
        (compound) -> new SmallWideSign(
            CoreData.SERIALIZER.read(compound.getCompound("CoreData")),
            compound.getString("Text")
        )
    );

    private String text;

    public SmallWideSign(
        CoreData coreData,
        String text
    ){
        super(coreData);
        this.text = text;
    }

    public SmallWideSign(
        Angle angle,
        String text,
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

    public void setText(String text) { this.text = text; }

    public String getText() { return text; }

    @Override
    protected void regenerateTransformedBox() {
        transformedBounds = new TransformedBox(LOCAL_BOUNDS).rotateAlong(Matrix4x4.Axis.Y, coreData.angle);
        if(coreData.flip) transformedBounds = transformedBounds.scale(new Vector3(1, 1, -1));
    }

    private void notifyTextChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        compound.putString("Text", text);
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
        if (compound.contains("Text")) {
            setText(compound.getString("Text"));
        }
        super.readMutationUpdate(compound.getCompound("CoreData"), tile, editingPlayer);
    }

    @Override
    public BlockPartMetadata<SmallWideSign> getMeta() {
        return METADATA;
    }

    @Override
    public void writeTo(CompoundNBT compound) {
        METADATA.write(this, compound);
    }

}
