package gollorum.signpost.modIntegration;

import java.util.Set;

import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.StonedHashSet;
import net.blay09.mods.waystones.PlayerWaystoneData;
import net.blay09.mods.waystones.WaystoneManager;
import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class WaystonesModHandler implements ModHandler {

	@Override
	public Set<BaseInfo> getAllBaseInfos() {
		StonedHashSet ret = new StonedHashSet();
		for(WaystoneEntry entry: WaystoneManager.getServerWaystones()){
			ret.add(baseInfoFromWaystoneEntry(entry));
		}
		return ret;
	}

	@Override
	public Set<BaseInfo> getAllBaseInfosByPlayer(EntityPlayer player) {
		NBTTagCompound tagCompound = PlayerWaystoneData.getWaystonesTag(player);
//		NBTTagList tagList = tagCompound.getTagList("WaystoneList", 10);
//		StonedHashSet ret = new StonedHashSet();
//		System.out.println("player "+player+" has "+tagList.tagCount()+" waystones waystones:");
//		for (int i = 0; i < tagList.tagCount(); ++i) {
//			NBTTagCompound entryCompound = tagList.getCompoundTagAt(i);
//			WaystoneEntry playerEntry = WaystoneEntry.read(entryCompound);
//			BaseInfo wrappedWaystone = baseInfoFromWaystoneEntry(playerEntry);
//			System.out.println(wrappedWaystone);
//			ret.add(wrappedWaystone);
//		}
//		return ret;
		return getAllBaseInfos();
	}
	
	private BaseInfo baseInfoFromWaystoneEntry(WaystoneEntry entry){
		if(entry==null){
			return null;
		}
		String name = entry.getName();
		
		int blockX = entry.getPos().getX();
		int blockY = entry.getPos().getY();
		int blockZ = entry.getPos().getZ();
		
		int teleX = blockX+1;
		int teleY = blockY;
		int teleZ = blockZ;
		
		int dim = entry.getDimensionId();
		return BaseInfo.fromExternal(name, blockX, blockY, blockZ, teleX, teleY, teleZ, dim, Waystones.MOD_ID);
	}
}