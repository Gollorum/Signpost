package gollorum.signpost.utils;

import gollorum.signpost.interactions.Interactable;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.security.WithOwner;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;
import java.util.Collection;

public interface BlockPart<T extends BlockPart<T>> extends Interactable {

    BlockPartMetadata<T> getMeta();

    default CompoundNBT write(){
        CompoundNBT compound = new CompoundNBT();
        writeTo(compound);
        return compound;
    }
    void writeTo(CompoundNBT compound);

    void readMutationUpdate(CompoundNBT compound, TileEntity tile, @Nullable PlayerEntity editingPlayer);
    boolean hasThePermissionToEdit(WithOwner tile, @Nullable PlayerEntity player);

    Collection<ItemStack> getDrops(PostTile tile);

    default void removeFrom(PostTile tile) {}

}