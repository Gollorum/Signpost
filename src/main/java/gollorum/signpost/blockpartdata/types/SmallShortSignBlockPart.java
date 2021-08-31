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
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

public class SmallShortSignBlockPart extends SignBlockPart<SmallShortSignBlockPart> {

    private static final AABB LOCAL_BOUNDS = new AABB(
        new Vector3(2, -11, 0.5f),
        new Vector3(18, -5, -0.5f)
    ).map(CoordinatesUtil::voxelToLocal);

    public static final BlockPartMetadata<SmallShortSignBlockPart> METADATA = new BlockPartMetadata<>(
        "small_short_sign",
        (sign, compound) -> {
            compound.put("CoreData", CoreData.SERIALIZER.write(sign.coreData));
            compound.putString("Text", sign.text);
        },
        (compound) -> new SmallShortSignBlockPart(
            CoreData.SERIALIZER.read(compound.getCompound("CoreData")),
            compound.getString("Text")
        ),
        SmallShortSignBlockPart.class
    );

    private String text;

    public SmallShortSignBlockPart(
        CoreData coreData,
        String text
    ){
        super(coreData);
        this.text = text;
    }

    public SmallShortSignBlockPart(
        Angle angle,
        String text,
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

    public void setText(String text) { this.text = text; }

    public String getText() { return text; }

    @Override
    protected void regenerateTransformedBox() {
        transformedBounds = new TransformedBox(LOCAL_BOUNDS).rotateAlong(Matrix4x4.Axis.Y, coreData.angle);
    }

    private void notifyTextChanged(InteractionInfo info) {
        CompoundTag compound = new CompoundTag();
        compound.putString("Text", text);
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
            editingPlayer.sendMessage(new TranslatableComponent(LangKeys.noPermissionSignpost), Util.NIL_UUID);
            return;
        }
        if (compound.contains("Text")) {
            setText(compound.getString("Text"));
        }
        super.readMutationUpdate(compound, tile, editingPlayer);
    }

    @Override
    public SmallShortSignBlockPart copy() {
        return new SmallShortSignBlockPart(coreData.copy(), text);
    }

    @Override
    public BlockPartMetadata<SmallShortSignBlockPart> getMeta() {
        return METADATA;
    }

    @Override
    public void writeTo(CompoundTag compound) {
        METADATA.write(this, compound);
    }

}
