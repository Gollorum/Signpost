package gollorum.signpost.minecraft.storage;

import gollorum.signpost.BlockRestrictions;
import gollorum.signpost.Signpost;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public class BlockRestrictionsStorage extends SavedData {

	public static final String NAME = Signpost.MOD_ID + "_BlockRestrictions";

	@Override
	public CompoundTag save(CompoundTag compound) {
		return BlockRestrictions.getInstance().saveTo(compound);
	}

	public BlockRestrictionsStorage load(CompoundTag compound) {
		BlockRestrictions.getInstance().readFrom(compound);
		return this;
	}

}
