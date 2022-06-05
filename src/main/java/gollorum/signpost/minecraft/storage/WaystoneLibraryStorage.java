package gollorum.signpost.minecraft.storage;

import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.worldgen.WaystoneJigsawPiece;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

public class WaystoneLibraryStorage extends SavedData {

    public static final String NAME = Signpost.MOD_ID + "_WaystoneLibrary";

    @Override
    public CompoundTag save(CompoundTag compound) {
        WaystoneLibrary.getInstance().saveTo(compound);
        compound.put("villageWaystones", WaystoneJigsawPiece.serialize());
        return compound;
    }

    public static WaystoneLibraryStorage load(CompoundTag compound) {
        WaystoneLibrary.getInstance().readFrom(compound);
        Tag villageWaystones = compound.get("villageWaystones");
        if(villageWaystones instanceof ListTag)
            WaystoneJigsawPiece.deserialize((ListTag) villageWaystones);
        return new WaystoneLibraryStorage();
    }

}
