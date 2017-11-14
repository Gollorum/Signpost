package gollorum.signpost.management;

import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.StonedHashSet;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.DimensionManager;

public class WorldSigns extends WorldSavedData{

	final static String key = "Signpost.WorldSigns";
	private World world;
	
	public WorldSigns(String tagName) {
		super(tagName);
	}
	
	public WorldSigns() {
		super(key);
	}

	public static WorldSigns worldSigns(World world){
		MapStorage storage = world.getPerWorldStorage();
		WorldSigns ret = (WorldSigns) storage.getOrLoadData(WorldSigns.class, key);
		if(ret == null){
			ret = new WorldSigns();
			storage.setData(key, ret);
		}
		ret.world = world;
		ret.markDirty();
		return ret;
	}

	@Override
	public void readFromNBT(NBTTagCompound tC) {
		NBTTagCompound info = (NBTTagCompound) tC.getTag("SignInfo");
		if(info!=null){
			int infoSize = info.getInteger("infoSize");
			StonedHashSet bases = new StonedHashSet();
			for(int i = 0; i<infoSize; i++){
				NBTTagCompound nowInfo = (NBTTagCompound) info.getTag("Base"+i);
				BaseInfo neu = BaseInfo.readFromNBT(nowInfo);
				if(neu.name!=null){
					bases.add(neu);
				}
			}
			PostHandler.allWaystones.addAll(bases);
			PostHandler.refreshDiscovered();
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tC) {
		if(world.equals(DimensionManager.getWorld(0))){
			NBTTagCompound info = new NBTTagCompound();
			StonedHashSet worldBases = PostHandler.allWaystones;
//			StonedHashSet worldBases = PostHandler.getByWorld(world);
			info.setInteger("infoSize", worldBases.size());
			int i = 0;
			for(BaseInfo now: worldBases){
				NBTTagCompound nowInfo = new NBTTagCompound();
				now.writeToNBT(nowInfo);
				info.setTag("Base"+i, nowInfo);
				i++;
			}
			tC.setTag("SignInfo", info);
		}
		return tC;
	}

}
