package gollorum.signpost.minecraft.storage;

import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.worldgen.VillageWaystone;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

public class WaystoneLibraryStorage extends SavedData {

    public static final String NAME = Signpost.MOD_ID + "_WaystoneLibrary";

    @Override
    public CompoundTag save(CompoundTag compound, HolderLookup.Provider provider) {
        WaystoneLibrary.getInstance().saveTo(compound, provider);
        compound.put("villageWaystones", VillageWaystone.serialize(provider));
        return compound;
    }

    public WaystoneLibraryStorage load(CompoundTag compound, HolderLookup.Provider registryAccess) {
        WaystoneLibrary.getInstance().readFrom(compound, registryAccess);
        Tag villageWaystones = compound.get("villageWaystones");
        if(villageWaystones instanceof ListTag)
            VillageWaystone.deserialize((ListTag) villageWaystones, registryAccess);
        return this;
    }

}
