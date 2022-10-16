package gollorum.signpost.blockpartdata.types;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.utils.CoordinatesUtil;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.utils.AngleProvider;
import gollorum.signpost.utils.NameProvider;
import gollorum.signpost.utils.math.geometry.AABB;
import gollorum.signpost.utils.math.geometry.Matrix4x4;
import gollorum.signpost.utils.math.geometry.TransformedBox;
import gollorum.signpost.utils.math.geometry.Vector3;
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
            compound.put("Text0", NameProvider.Serializer.write(sign.text[0]));
            compound.put("Text1", NameProvider.Serializer.write(sign.text[1]));
            compound.put("Text2", NameProvider.Serializer.write(sign.text[2]));
            compound.put("Text3", NameProvider.Serializer.write(sign.text[3]));
        },
        (compound) -> new LargeSignBlockPart(
            CoreData.SERIALIZER.read(compound.getCompound("CoreData")),
            new NameProvider[]{
                NameProvider.fetchFrom(compound.get("Text0")),
                NameProvider.fetchFrom(compound.get("Text1")),
                NameProvider.fetchFrom(compound.get("Text2")),
                NameProvider.fetchFrom(compound.get("Text3"))}
        ), LargeSignBlockPart.class);

    private NameProvider[] text;

    public LargeSignBlockPart(
        CoreData coreData,
        NameProvider[] text
    ) {
        super(coreData);
        assert text.length == 4;
        this.text = text;
    }

    public LargeSignBlockPart(
        AngleProvider angle,
        NameProvider[] text,
        boolean flip,
        ResourceLocation mainTexture,
        ResourceLocation secondaryTexture,
        Optional<Overlay> overlay,
        int color,
        Optional<WaystoneHandle> destination,
        ItemStack itemToDropOnBreak,
        PostBlock.ModelType modelType,
        boolean isLocked,
        boolean isMarkedForGeneration
    ) { this(
        new CoreData(angle, flip, mainTexture, secondaryTexture, overlay,
            color, destination, modelType, itemToDropOnBreak, isLocked, isMarkedForGeneration),
        text
    ); }

    public void setText(NameProvider[] text) {
        this.text = text;
    }

    public NameProvider[] getText() { return text; }

    @Override
    protected NameProvider[] getNameProviders() {
        return text;
    }

    @Override
    protected void regenerateTransformedBox() {
        transformedBounds = new TransformedBox(LOCAL_BOUNDS).rotateAlong(Matrix4x4.Axis.Y, coreData.angleProvider.get());
        if(coreData.flip) transformedBounds = transformedBounds.scale(new Vector3(1, 1, -1));
    }

    private void notifyTextChanged(InteractionInfo info) {
        CompoundTag compound = new CompoundTag();
        compound.put("Text0", NameProvider.Serializer.write(text[0]));
        compound.put("Text1", NameProvider.Serializer.write(text[1]));
        compound.put("Text2", NameProvider.Serializer.write(text[2]));
        compound.put("Text3", NameProvider.Serializer.write(text[3]));
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
            text[0] = NameProvider.fetchFrom(compound.get("Text0"));
        }
        if (compound.contains("Text1")) {
            text[1] = NameProvider.fetchFrom(compound.get("Text1"));
        }
        if (compound.contains("Text2")) {
            text[2] = NameProvider.fetchFrom(compound.get("Text2"));
        }
        if (compound.contains("Text3")) {
            text[3] = NameProvider.fetchFrom(compound.get("Text3"));
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
