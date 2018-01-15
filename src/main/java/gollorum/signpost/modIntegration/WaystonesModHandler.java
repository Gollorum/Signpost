package gollorum.signpost.modIntegration;

import java.io.File;
import java.io.FileInputStream;
import java.util.Set;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import gollorum.signpost.Signpost;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.StonedHashSet;
import net.blay09.mods.waystones.PlayerWaystoneData;
import net.blay09.mods.waystones.WaystoneManager;
import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.DimensionManager;

public class WaystonesModHandler implements ModHandler {

	@Override
	public Set<BaseInfo> getAllBaseInfos() {
		Set<BaseInfo> ret = getGlobal();
		ret.addAll(getNotGlobalByAllPlayers());
		return ret;
	}

	@Override
	public Set<BaseInfo> getAllBaseInfosByPlayer(EntityPlayer player) {
		Set<BaseInfo> ret = getGlobal();
		ret.addAll(getNotGlobalByPlayer(player));
		return ret;
	}
	
	private Set<BaseInfo> getGlobal(){
		StonedHashSet ret = new StonedHashSet();
		for(WaystoneEntry entry: WaystoneManager.getServerWaystones()){
			ret.add(baseInfoFromWaystoneEntry(entry));
		}
		return ret;
	}
	
	private Set<BaseInfo> getNotGlobalByPlayer(EntityPlayer player){
		StonedHashSet ret = new StonedHashSet();
		try{
			NBTTagCompound tagCompound = PlayerWaystoneData.getWaystonesTag(player);
			ret.addAll(getNotGlobalByTagCompound(tagCompound));
		}catch(Exception e){}
		return ret;
	}
	
	private Set<BaseInfo> getNotGlobalByAllPlayers(){
		Set<BaseInfo> ret = new StonedHashSet();
		for(EntityPlayer player: Signpost.proxy.getAllPlayers()){
			ret.addAll(getNotGlobalByPlayer(player));
		}
		if(FMLCommonHandler.instance().getEffectiveSide().equals(Side.CLIENT)){
			return ret;
		}else{
			File playerDirectory = new File(DimensionManager.getWorld(0).getSaveHandler().getWorldDirectory(), "playerdata");
			if(playerDirectory.isDirectory()){
				for(File file: playerDirectory.listFiles()){
					try{
						FileInputStream stream = new FileInputStream(file);
						NBTTagCompound tagCompound = CompressedStreamTools.readCompressed(stream);
						tagCompound = tagCompound.getCompoundTag("ForgeData").getCompoundTag("PlayerPersisted").getCompoundTag("Waystones");
						stream.close();
						Set<BaseInfo> bases = getNotGlobalByTagCompound(tagCompound);
						ret.addAll(bases);
					}catch(Exception e){e.printStackTrace();}
				}
			}
			return ret;
		}
	}
	
	private Set<BaseInfo> getNotGlobalByTagCompound(NBTTagCompound tagCompound){
		Set<BaseInfo> ret = new StonedHashSet();
		NBTTagList tagList = tagCompound.getTagList("WaystoneList", 10);
		for (int i = 0; i < tagList.tagCount(); ++i) {
			NBTTagCompound entryCompound = tagList.getCompoundTagAt(i);
			WaystoneEntry playerEntry = WaystoneEntry.read(entryCompound);
			BaseInfo wrappedWaystone = baseInfoFromWaystoneEntry(playerEntry);
			ret.add(wrappedWaystone);
		}
		return ret;
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