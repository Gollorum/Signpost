package gollorum.signpost.management;

import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.MyBlockPosSet;
import gollorum.signpost.util.StringSet;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

public class PlayerStore{

	public EntityPlayerMP player;
	
	public void init(EntityPlayerMP player){
		this.player = player;
	}
	
	public NBTTagCompound saveNBTData(NBTTagCompound compound) {
		StringSet knownOld = PostHandler.getPlayerKnownWaystoneNames(player.getUniqueID());
		compound.setInteger("knownCount", knownOld.size());
		int i = 0;
		for (String now : knownOld) {
			compound.setString("ws" + (i++), now);
		}
		PlayerRestrictions known = PostHandler.getPlayerKnownWaystonePositions(player.getUniqueID());
		compound.setInteger("knownCountPos", known.discoveredWastones.size());
		i = 0;
		for (MyBlockPos now : known.discoveredWastones) {
			compound.setTag("wsPos" + (i++), now.writeToNBT(new NBTTagCompound()));
		}
		compound.setInteger("leftWaystones", known.remainingWaystones);
		compound.setInteger("leftSignposts", known.remainingSignposts);
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

		PlayerRestrictions known = PostHandler.playerKnownWaystonePositions.get(player.getUniqueID());
		if(compound.hasKey("leftWaystones")){
			known.remainingWaystones = compound.getInteger("leftWaystones");
		}else{
			known.remainingWaystones = ClientConfigStorage.INSTANCE.getMaxWaystones();
		}
		if(compound.hasKey("leftSignposts")){
			known.remainingSignposts = compound.getInteger("leftSignposts");
		}else{
			known.remainingSignposts = ClientConfigStorage.INSTANCE.getMaxSignposts();
		}
	}

}
