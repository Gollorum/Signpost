package gollorum.signpost.management;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;

public class PlayerStorage implements IStorage<PlayerStore>{

	@Override
	public NBTBase writeNBT(Capability<PlayerStore> capability, PlayerStore instance, EnumFacing side) {
		return instance.saveNBTData(new NBTTagCompound());
	}

	@Override
	public void readNBT(Capability<PlayerStore> capability, PlayerStore instance, EnumFacing side, NBTBase nbt) {
		instance.loadNBTData((NBTTagCompound)nbt);
	}

}
