package gollorum.signpost.management;

import gollorum.signpost.util.StringSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

public class PlayerStore implements IExtendedEntityProperties {

	public EntityPlayerMP player;

	@Override
	public void saveNBTData(NBTTagCompound compound) {
		System.out.println("s NBT d");
		StringSet known = PostHandler.playerKnownWaystones.get(player.getUniqueID()).a;
		if (known != null) {
			System.out.println(known.size());
			compound.setInteger("knownCount", known.size());
			int i = 0;
			for (String now : known) {
				compound.setString("ws" + (i++), now);
			}
		}else{
			System.out.println("null");
			compound.setInteger("knownCount", 0);
		}
	}

	@Override
	public void loadNBTData(NBTTagCompound compound) {
		StringSet toBeAdded = new StringSet();
		int c = compound.getInteger("knownCount");
		for (int i = 0; i < c; i++) {
			toBeAdded.add(compound.getString("ws" + i));
		}
		PostHandler.addAllDiscoveredByName(player.getUniqueID(), toBeAdded);
	}

	@Override
	public void init(Entity entity, World world) {
		player = (EntityPlayerMP) entity;
	}

}
