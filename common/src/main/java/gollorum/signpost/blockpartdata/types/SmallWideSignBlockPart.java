package gollorum.signpost.blockpartdata.types;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.blockpartdata.Overlay;
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

public class SmallWideSignBlockPart extends SignBlockPart<SmallWideSignBlockPart> {

    private static final AABB LOCAL_BOUNDS = new AABB(
        new Vector3(-9, -11, 2),
        new Vector3(16, -5, 3)
    ).map(CoordinatesUtil::voxelToLocal);

    public static final BlockPartMetadata<SmallWideSignBlockPart> METADATA = new BlockPartMetadata<>(
        "small_wide_sign",
        (sign, compound, provider) -> {
            compound.put("CoreData", CoreData.SERIALIZER.encode(sign.coreData, provider));
            compound.put("Text", NameProvider.COMPOUND_SERIALIZER.encode(sign.text, provider));
        },
        (compound, provider) -> new SmallWideSignBlockPart(
            CoreData.SERIALIZER.decode(compound.getCompound("CoreData"), provider),
            NameProvider.fetchFrom(compound.get("Text"), provider)
        ),
        SmallWideSignBlockPart.class
    );

    private NameProvider text;

    public SmallWideSignBlockPart(
        CoreData coreData,
        NameProvider text
    ){
        super(coreData);
        this.text = text;
    }

    public SmallWideSignBlockPart(
        AngleProvider angle,
        NameProvider text,
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

    public void setText(NameProvider text) { this.text = text; }

    public NameProvider getText() { return text; }

    @Override
    protected NameProvider[] getNameProviders() {
        return new NameProvider[]{text};
    }

    @Override
    protected void regenerateTransformedBox() {
        transformedBounds = new TransformedBox(LOCAL_BOUNDS).rotateAlong(Matrix4x4.Axis.Y, coreData.angleProvider.get());
        if(coreData.flip) transformedBounds = transformedBounds.scale(new Vector3(1, 1, -1));
    }

//    private void notifyTextChanged(InteractionInfo info) {
//        CompoundTag compound = new CompoundTag();
//        compound.put("Text", NameProvider.Serializer.encode(text));
//        info.mutationDistributor.accept(compound);
//    }

    @Override
    public void readMutationUpdate(CompoundTag compound, BlockEntity tile, Player editingPlayer, HolderLookup.Provider provider) {
        if(editingPlayer != null
            && !editingPlayer.level().isClientSide()
            && tile instanceof WithOwner.OfSignpost
            && !hasThePermissionToEdit(((WithOwner.OfSignpost)tile), editingPlayer)
        ) {
            // This should not happen unless a sender tries to hacc
            editingPlayer.displayClientMessage(Component.translatable(LangKeys.noPermissionSignpost), false);
            return;
        }
        if (compound.contains("Text")) {
            setText(NameProvider.fetchFrom(compound.get("Text"), provider));
        }
        super.readMutationUpdate(compound, tile, editingPlayer, provider);
    }

    @Override
    public SmallWideSignBlockPart copy() {
        return new SmallWideSignBlockPart(coreData.copy(), text);
    }

    @Override
    public BlockPartMetadata<SmallWideSignBlockPart> getMeta() {
        return METADATA;
    }

    @Override
    public void writeTo(CompoundTag compound, HolderLookup.Provider provider) {
        METADATA.encode(compound, this, provider);
    }

}
