package gollorum.signpost.management;

import gollorum.signpost.Signpost;
import gollorum.signpost.util.StringSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class PlayerStore{

	public StringSet known;
	
	public static ResourceLocation loc = new ResourceLocation(Signpost.MODID+":Playerstore");

	public void init(EntityPlayerMP player){
		known = PostHandler.playerKnownWaystones.get(player.getUniqueID());
		if(known == null){
			known = new StringSet();
			PostHandler.playerKnownWaystones.put(player.getUniqueID(), known);
		}
	}
	
	public NBTTagCompound saveNBTData(NBTTagCompound compound) {
		if (known != null) {
			compound.setInteger("knownCount", known.size());
			int i = 0;
			for (String now : known) {
				compound.setString("ws" + (i++), now);
			}
		}else{
			compound.setInteger("knownCount", 0);
		}
		return compound;
	}

	public void loadNBTData(NBTTagCompound compound) {
		known = new StringSet();
		int c = compound.getInteger("knownCount");
		System.out.println("LADE "+c);
		for (int i = 0; i < c; i++) {
			known.add(compound.getString("ws" + i));
		}
	}

}
