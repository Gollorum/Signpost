package gollorum.signpost.management;

import gollorum.signpost.util.StringSet;
import gollorum.signpost.util.collections.Pair;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

public class PlayerStore{

//	public StringSet known;
//	
//	public static ResourceLocation loc = new ResourceLocation(Signpost.MODID+":Playerstore");
	
	public EntityPlayerMP player;

	public void init(EntityPlayerMP player){
		this.player = player;
	}
	
	public NBTTagCompound saveNBTData(NBTTagCompound compound) {
		Pair<StringSet, Pair<Integer, Integer>> known = PostHandler.playerKnownWaystones.get(player.getUniqueID());
		compound.setInteger("knownCount", known.a.size());
		int i = 0;
		for (String now : known.a) {
			compound.setString("ws" + (i++), now);
		}
		compound.setInteger("leftWaystones", known.b.a);
		compound.setInteger("leftSignposts", known.b.b);
		return compound;
	}

	public void loadNBTData(NBTTagCompound compound) {
		StringSet toBeAdded = new StringSet();
		int c = compound.getInteger("knownCount");
		for (int i = 0; i < c; i++) {
			String getString = compound.getString("ws" + i);
			toBeAdded.add(getString);
			System.out.println(getString);
		}
		PostHandler.addAllDiscoveredByName(player.getUniqueID(), toBeAdded);
		Pair<Integer, Integer> pair = PostHandler.playerKnownWaystones.get(player.getUniqueID()).b;
		if(compound.hasKey("leftWaystones")){
			pair.a = compound.getInteger("leftWaystones");
		}else{
			pair.a = ConfigHandler.maxWaystones;
		}
		if(compound.hasKey("leftSignposts")){
			pair.b = compound.getInteger("leftSignposts");
		}else{
			pair.b = ConfigHandler.maxSignposts;
		}
	}

}
