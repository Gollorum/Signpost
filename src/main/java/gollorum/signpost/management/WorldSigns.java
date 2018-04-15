package gollorum.signpost.management;

import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.StonedHashSet;
import gollorum.signpost.worldGen.villages.VillageLibrary;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

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
				if(neu.getName()!=null){
					bases.add(neu);
				}
			}
			PostHandler.getNativeWaystones().addAll(bases);
			PostHandler.refreshDiscovered();  
			if (tC.hasKey("VillageLibrary")) {
				VillageLibrary.getInstance().load(tC.getCompoundTag("VillageLibrary"));
			}
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tC) {
		if(world.provider.getDimension() == 0){
			NBTTagCompound info = new NBTTagCompound();
			StonedHashSet worldBases = PostHandler.getNativeWaystones();
			info.setInteger("infoSize", worldBases.size());
			int i = 0;
			for(BaseInfo now: worldBases){
				NBTTagCompound nowInfo = new NBTTagCompound();
				now.writeToNBT(nowInfo);
				info.setTag("Base"+i, nowInfo);
				i++;
			}
			tC.setTag("SignInfo", info); 
			NBTTagCompound library = new NBTTagCompound();
			VillageLibrary.getInstance().save(library);
			tC.setTag("VillageLibrary", library); 
		}
		return tC;
	}

}
