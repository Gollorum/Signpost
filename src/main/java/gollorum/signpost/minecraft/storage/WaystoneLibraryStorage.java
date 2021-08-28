package gollorum.signpost.minecraft.storage;

import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.worldgen.WaystoneJigsawPiece;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.world.storage.WorldSavedData;

public class WaystoneLibraryStorage extends WorldSavedData {

    public static final String NAME = Signpost.MOD_ID + "_WaystoneLibrary";

    public WaystoneLibraryStorage() { super(NAME); }

    @Override
    public CompoundNBT save(CompoundNBT compound) {
        WaystoneLibrary.getInstance().saveTo(compound);
        compound.put("villageWaystones", WaystoneJigsawPiece.serialize());
        return compound;
    }

    @Override
    public void load(CompoundNBT compound) {
        WaystoneLibrary.getInstance().readFrom(compound);
        INBT villageWaystones = compound.get("villageWaystones");
        if(villageWaystones instanceof ListNBT)
            WaystoneJigsawPiece.deserialize((ListNBT) villageWaystones);
    }

}
