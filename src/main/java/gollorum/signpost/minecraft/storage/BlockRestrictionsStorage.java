package gollorum.signpost.minecraft.storage;

import gollorum.signpost.BlockRestrictions;
import gollorum.signpost.Signpost;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.storage.WorldSavedData;

public class BlockRestrictionsStorage extends WorldSavedData {

	public static final String NAME = Signpost.MOD_ID + "_BlockRestrictions";

	public BlockRestrictionsStorage() { super(NAME); }

	@Override
	public CompoundNBT save(CompoundNBT compound) {
		return BlockRestrictions.getInstance().saveTo(compound);
	}

	@Override
	public void load(CompoundNBT compound) {
		BlockRestrictions.getInstance().readFrom(compound);
	}

}
