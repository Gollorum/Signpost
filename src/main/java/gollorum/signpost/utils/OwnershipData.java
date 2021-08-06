package gollorum.signpost.utils;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundNBT;

public class OwnershipData {

	public static final OwnershipData empty = new OwnershipData(PlayerHandle.Invalid, false);
	public final PlayerHandle owner;
	public final boolean isLocked;

	public OwnershipData(PlayerHandle owner, boolean isLocked) {
		this.owner = owner;
		this.isLocked = isLocked;
	}

	public static final CompoundSerializable<OwnershipData> Serializer = new SerializerImpl();
	public static final class SerializerImpl implements CompoundSerializable<OwnershipData> {
		@Override
		public Class<OwnershipData> getTargetClass() {
			return OwnershipData.class;
		}

		@Override
		public CompoundNBT write(
			OwnershipData ownershipData, CompoundNBT compound
		) {
			PlayerHandle.Serializer.write(ownershipData.owner, compound);
			compound.putBoolean("locked", ownershipData.isLocked);
			return compound;
		}

		@Override
		public boolean isContainedIn(CompoundNBT compound) {
			return PlayerHandle.Serializer.isContainedIn(compound)
				&& compound.contains("locked");
		}

		@Override
		public OwnershipData read(CompoundNBT compound) {
			return new OwnershipData(
				PlayerHandle.Serializer.read(compound),
				compound.getBoolean("locked")
			);
		}
	};

}
