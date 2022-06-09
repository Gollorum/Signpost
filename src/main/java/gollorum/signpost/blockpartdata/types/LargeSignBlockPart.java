package gollorum.signpost.blockpartdata.types;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.utils.CoordinatesUtil;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.AABB;
import gollorum.signpost.utils.math.geometry.Matrix4x4;
import gollorum.signpost.utils.math.geometry.TransformedBox;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

public class LargeSignBlockPart extends SignBlockPart<LargeSignBlockPart> {

    private static final AABB LOCAL_BOUNDS = new AABB(
        new Vector3(-9, -14, 2),
        new Vector3(12, -2, 3)
    ).map(CoordinatesUtil::voxelToLocal);

    public static final BlockPartMetadata<LargeSignBlockPart> METADATA = new BlockPartMetadata<>(
        "large_sign",
        (sign, compound) -> {
            compound.put("CoreData", CoreData.SERIALIZER.write(sign.coreData));
            compound.putString("Text0", sign.text[0]);
            compound.putString("Text1", sign.text[1]);
            compound.putString("Text2", sign.text[2]);
            compound.putString("Text3", sign.text[3]);
        },
        (compound) -> new LargeSignBlockPart(
            CoreData.SERIALIZER.read(compound.getCompound("CoreData")),
            new String[]{
                compound.getString("Text0"),
                compound.getString("Text1"),
                compound.getString("Text2"),
                compound.getString("Text3")}
        ), LargeSignBlockPart.class);

    private String[] text;

    public LargeSignBlockPart(
        CoreData coreData,
        String[] text
    ) {
        super(coreData);
        assert text.length == 4;
        this.text = text;
    }

    public LargeSignBlockPart(
        Angle angle,
        String[] text,
        boolean flip,
        ResourceLocation mainTexture,
        ResourceLocation secondaryTexture,
        Optional<Overlay> overlay,
        int color,
        Optional<WaystoneHandle> destination,
        ItemStack itemToDropOnBreak,
        PostBlock.ModelType modelType,
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
        CompoundTag compound = new CompoundTag();
        compound.putString("Text0", text[0]);
        compound.putString("Text1", text[1]);
        compound.putString("Text2", text[2]);
        compound.putString("Text3", text[3]);
        info.mutationDistributor.accept(compound);
    }

    @Override
    public void readMutationUpdate(CompoundTag compound, BlockEntity tile, Player editingPlayer) {
        if(editingPlayer != null
            && !editingPlayer.level.isClientSide()
            && tile instanceof WithOwner.OfSignpost
            && !hasThePermissionToEdit(((WithOwner.OfSignpost)tile), editingPlayer)
        ) {
            // This should not happen unless a player tries to hacc
            editingPlayer.sendSystemMessage(Component.translatable(LangKeys.noPermissionSignpost));
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
    public LargeSignBlockPart copy() {
        return new LargeSignBlockPart(coreData.copy(), text);
    }

    @Override
    public BlockPartMetadata<LargeSignBlockPart> getMeta() {
        return METADATA;
    }

    @Override
    public void writeTo(CompoundTag compound) {
        METADATA.write(this, compound);
    }

}
