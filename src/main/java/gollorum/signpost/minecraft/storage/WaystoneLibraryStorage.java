package gollorum.signpost.minecraft.storage;

import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneLibrary;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.storage.WorldSavedData;

public class WaystoneLibraryStorage extends WorldSavedData {

    public static final String NAME = Signpost.MOD_ID + "_WaystoneLibrary";

    public WaystoneLibraryStorage() { super(NAME); }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        return WaystoneLibrary.getInstance().saveTo(compound);
    }

    @Override
    public void read(CompoundNBT compound) {
        WaystoneLibrary.getInstance().readFrom(compound);
    }

}
