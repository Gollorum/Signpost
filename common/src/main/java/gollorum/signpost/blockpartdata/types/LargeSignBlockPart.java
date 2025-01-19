package gollorum.signpost.blockpartdata.types;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.utils.CoordinatesUtil;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.Texture;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.utils.AngleProvider;
import gollorum.signpost.utils.NameProvider;
import gollorum.signpost.utils.math.geometry.AABB;
import gollorum.signpost.utils.math.geometry.Matrix4x4;
import gollorum.signpost.utils.math.geometry.TransformedBox;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
        (sign, compound, provider) -> {
            compound.put("CoreData", CoreData.SERIALIZER.encode(sign.coreData, provider));
            compound.put("Text0", NameProvider.COMPOUND_SERIALIZER.encode(sign.text[0], provider));
            compound.put("Text1", NameProvider.COMPOUND_SERIALIZER.encode(sign.text[1], provider));
            compound.put("Text2", NameProvider.COMPOUND_SERIALIZER.encode(sign.text[2], provider));
            compound.put("Text3", NameProvider.COMPOUND_SERIALIZER.encode(sign.text[3], provider));
        },
        (compound, provider) -> new LargeSignBlockPart(
            CoreData.SERIALIZER.decode(compound.getCompound("CoreData"), provider),
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
        Texture mainTexture,
        Texture secondaryTexture,
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

    private void notifyTextChanged(InteractionInfo info, HolderLookup.Provider provider) {
        CompoundTag compound = new CompoundTag();
        compound.put("Text0", NameProvider.COMPOUND_SERIALIZER.encode(text[0], provider));
        compound.put("Text1", NameProvider.COMPOUND_SERIALIZER.encode(text[1], provider));
        compound.put("Text2", NameProvider.COMPOUND_SERIALIZER.encode(text[2], provider));
        compound.put("Text3", NameProvider.COMPOUND_SERIALIZER.encode(text[3], provider));
        info.mutationDistributor.accept(compound);
    }

    @Override
    public void readMutationUpdate(CompoundTag compound, BlockEntity tile, Player editingPlayer, HolderLookup.Provider provider) {
        if(editingPlayer != null
            && !editingPlayer.level().isClientSide()
            && tile instanceof WithOwner.OfSignpost
            && !hasThePermissionToEdit(((WithOwner.OfSignpost)tile), editingPlayer)
        ) {
            // This should not happen unless a sender tries to hacc
            editingPlayer.displayClientMessage(Component.translatable(LangKeys.noPermissionSignpost), true);
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
        super.readMutationUpdate(compound, tile, editingPlayer, provider);
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
    public void writeTo(CompoundTag compound, HolderLookup.Provider provider) {
        METADATA.encode(compound, this, provider);
    }

}
