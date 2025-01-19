package gollorum.signpost.utils;

import gollorum.signpost.interactions.Interactable;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.utils.Texture;
import gollorum.signpost.security.WithOwner;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.Collection;

public interface BlockPart<T extends BlockPart<T>> extends Interactable {

    BlockPartMetadata<T> getMeta();

    default CompoundTag write(HolderLookup.Provider provider){
        CompoundTag compound = new CompoundTag();
        writeTo(compound, provider);
        return compound;
    }
    void writeTo(CompoundTag compound, HolderLookup.Provider provider);

    void readMutationUpdate(CompoundTag compound, BlockEntity tile, @Nullable Player editingPlayer, HolderLookup.Provider holderLookupProvider);
    boolean hasThePermissionToEdit(WithOwner tile, @Nullable Player player);

    Collection<ItemStack> getDrops(PostTile tile);

    default void attachTo(PostTile tile) {}
    default void removeFrom(PostTile tile) {}

    Collection<Texture> getAllTextures();

}