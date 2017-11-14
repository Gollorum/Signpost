package gollorum.signpost.management;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

public class PlayerProvider implements ICapabilitySerializable<NBTBase>{

	@CapabilityInject(PlayerStore.class)
	 public static final Capability<PlayerStore> STORE_CAP = null;

	 private PlayerStore instance = STORE_CAP.getDefaultInstance();

	 public EntityPlayerMP player;
	 
	 public PlayerProvider(EntityPlayerMP player){
		 this.player = player;
		 instance.player = player;
	 }
	
	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == STORE_CAP;
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		return capability == STORE_CAP ? STORE_CAP.<T> cast(this.instance) : null;
	}

	@Override
	public NBTBase serializeNBT() {
		return STORE_CAP.getStorage().writeNBT(STORE_CAP, instance, null);
	}

	@Override
	public void deserializeNBT(NBTBase nbt) {
		STORE_CAP.getStorage().readNBT(STORE_CAP, instance, null, nbt);
	}

}
