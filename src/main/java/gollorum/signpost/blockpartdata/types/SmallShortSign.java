package gollorum.signpost.blockpartdata.types;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.Post;
import gollorum.signpost.minecraft.utils.CoordinatesUtil;
import gollorum.signpost.minecraft.utils.LangKeys;
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

public class SmallShortSign extends Sign<SmallShortSign> {

    private static final AABB LOCAL_BOUNDS = new AABB(
        new Vector3(2, -11, 0.5f),
        new Vector3(18, -5, -0.5f)
    ).map(CoordinatesUtil::voxelToLocal);

    public static final BlockPartMetadata<SmallShortSign> METADATA = new BlockPartMetadata<>(
        "small_short_sign",
        (sign, compound) -> {
            compound.put("CoreData", CoreData.SERIALIZER.write(sign.coreData));
            compound.putString("Text", sign.text);
        },
        (compound) -> new SmallShortSign(
            CoreData.SERIALIZER.read(compound.getCompound("CoreData")),
            compound.getString("Text")
        )
    );

    private String text;

    public SmallShortSign(
        CoreData coreData,
        String text
    ){
        super(coreData);
        this.text = text;
    }

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
        Post.ModelType modelType,
        Optional<PlayerHandle> owner
    ) { this(
        new CoreData(angle, flip, mainTexture, secondaryTexture, overlay,
            color, destination, modelType, itemToDropOnBreak, owner),
        text
    ); }

    public void setText(String text) { this.text = text; }

    public String getText() { return text; }

    @Override
    protected void regenerateTransformedBox() {
        transformedBounds = new TransformedBox(LOCAL_BOUNDS).rotateAlong(Matrix4x4.Axis.Y, coreData.angle);
    }

    private void notifyTextChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        compound.putString("Text", text);
        info.mutationDistributor.accept(compound);
    }

    @Override
    public void readMutationUpdate(CompoundNBT compound, TileEntity tile, PlayerEntity editingPlayer) {
        if(editingPlayer != null && editingPlayer.isServerWorld() && !hasThePermissionToEdit(editingPlayer)) {
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
    public BlockPartMetadata<SmallShortSign> getMeta() {
        return METADATA;
    }

    @Override
    public void writeTo(CompoundNBT compound) {
        METADATA.write(this, compound);
    }

}
