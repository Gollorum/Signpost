package gollorum.signpost.management;

import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.MyBlockPosSet;
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
		StringSet knownOld = PostHandler.playerKnownWaystones.get(player.getUniqueID());
		compound.setInteger("knownCount", knownOld.size());
		int i = 0;
		for (String now : knownOld) {
			compound.setString("ws" + (i++), now);
		}
		Pair<MyBlockPosSet, Pair<Integer, Integer>> known = PostHandler.playerKnownWaystonePositions.get(player.getUniqueID());
		compound.setInteger("knownCountPos", known.a.size());
		i = 0;
		for (MyBlockPos now : known.a) {
			compound.setTag("wsPos" + (i++), now.writeToNBT(new NBTTagCompound()));
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
		}
		PostHandler.addAllDiscoveredByName(player.getUniqueID(), toBeAdded);
		MyBlockPosSet toBeAddedPos = new MyBlockPosSet();
		c = compound.getInteger("knownCountPos");
		for(int i=0; i<c; i++){
			MyBlockPos getPos = MyBlockPos.readFromNBT(compound.getCompoundTag("wsPos"+i));
			toBeAddedPos.add(getPos);
		}
		PostHandler.addAllDiscoveredByPos(player.getUniqueID(), toBeAddedPos);
		
		Pair<Integer, Integer> pair = PostHandler.playerKnownWaystonePositions.get(player.getUniqueID()).b;
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
