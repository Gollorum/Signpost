package gollorum.signpost.utils;

import gollorum.signpost.interactions.Interactable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;

public interface BlockPart<T extends BlockPart<T>> extends Interactable, Renderable {

    BlockPartMetadata<T> getMeta();

    default CompoundNBT write(){
        CompoundNBT compound = new CompoundNBT();
        writeTo(compound);
        return compound;
    }
    void writeTo(CompoundNBT compound, String keyPrefix);
    default void writeTo(CompoundNBT compound){
        writeTo(compound, "");
    }

    void readMutationUpdate(CompoundNBT compound, TileEntity tile);
}