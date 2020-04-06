package gollorum.signpost.worldGen.villages;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.code.MinecraftDependent;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@MinecraftDependent
public class VillageLibrary {

	private static VillageLibrary INSTANCE = new VillageLibrary();
	public static VillageLibrary getInstance(){
		return INSTANCE;
	}
	public static void init(){
		INSTANCE = new VillageLibrary();
	}
	
	private Map<MyBlockPos, MyBlockPos> villageWaystones;
	private Map<MyBlockPos, Set<VillagePost>> villagePosts;
	
	private VillageLibrary(){
		villageWaystones = new HashMap<>();
		villagePosts = new HashMap<>();
	}
	
	public void putWaystone(final MyBlockPos villageLocation, final MyBlockPos waystoneLocation){
		villageWaystones.put(villageLocation, waystoneLocation);
		SPEventHandler.scheduleTask(() -> {
			if(waystoneLocation.getTile() == null){
				return false;
			}else{
				new LibraryWaystoneHelper(villageLocation, villagePosts, waystoneLocation).enscribeEmptySign();
				return true;
			}
		});
	}

	public void putSignpost(final MyBlockPos villageLocation, final MyBlockPos signpostLocation, final double optimalRot){
		Set<VillagePost> villageSignposts = villagePosts.get(villageLocation);
		if(villageSignposts == null){
			villageSignposts = new HashSet<>();
			villagePosts.put(villageLocation, villageSignposts);
		}
		villageSignposts.add(new VillagePost(signpostLocation, optimalRot));
		SPEventHandler.scheduleTask(() -> {
			if(signpostLocation.getTile() == null){
				return false;
			}else{
				new LibrarySignpostHelper(villageLocation, signpostLocation, villageWaystones).enscribeNewSign(optimalRot);
				return true;
			}
		});
	}

	public void save(NBTTagCompound compound){
		compound.setTag("Waystones", saveWaystones());
		compound.setTag("Signposts", savePosts());
	}

	private NBTTagCompound saveWaystones() {
		NBTTagCompound compound = new NBTTagCompound();
		compound.setInteger("WaystoneCount", villageWaystones.size());
		int i=0;
		for(Entry<MyBlockPos, MyBlockPos> now: villageWaystones.entrySet()){
			compound.setTag("Waystone"+(i++), saveWaystone(now.getKey(), now.getValue()));
		}
		return compound;
	}

	private NBTBase saveWaystone(MyBlockPos villageLocation, MyBlockPos waystoneLocation) {
		NBTTagCompound compound = new NBTTagCompound();
		compound.setTag("VillageLocation", villageLocation.writeToNBT(new NBTTagCompound()));
		compound.setTag("WaystoneLocation", waystoneLocation.writeToNBT(new NBTTagCompound()));
		return compound;
	}
	
	private NBTTagCompound savePosts() {
		NBTTagCompound compound = new NBTTagCompound();
		compound.setInteger("PostCount", villagePosts.size());
		int i=0;
		for(Entry<MyBlockPos, Set<VillagePost>> now: villagePosts.entrySet()){
			compound.setTag("Posts"+(i++), savePostCollection(now.getKey(), now.getValue()));
		}
		return compound;
	}
	
	private NBTBase savePostCollection(MyBlockPos villageLocation, Set<VillagePost> posts) {
		NBTTagCompound compound = new NBTTagCompound();
		compound.setTag("VillageLocation", villageLocation.writeToNBT(new NBTTagCompound()));
		compound.setInteger("PostCount", posts.size());
		int i=0;
		for(VillagePost now: posts){
			compound.setTag("Post"+(i++), now.save());
		}
		return compound;
	}
	
	public void load(NBTTagCompound compound){
		loadWaystones(compound.getCompoundTag("Waystones"));
		loadSignpost(compound.getCompoundTag("Signposts"));
	}
	
	private void loadWaystones(NBTTagCompound compound) {
		villageWaystones = new HashMap<>();
		int count = compound.getInteger("WaystoneCount");
		for(int i=0; i<count; i++){
			NBTTagCompound entry = compound.getCompoundTag("Waystone"+i);
			MyBlockPos villageLocation = MyBlockPos.readFromNBT(entry.getCompoundTag("VillageLocation"));
			MyBlockPos waystoneLocation = MyBlockPos.readFromNBT(entry.getCompoundTag("WaystoneLocation"));
			villageWaystones.put(villageLocation, waystoneLocation);
		}
	}

	private void loadSignpost(NBTTagCompound compound) {
		villagePosts = new HashMap<>();
		int postCount = compound.getInteger("PostCount");
		for(int i=0; i<postCount; i++){
			NBTTagCompound entry = compound.getCompoundTag("Posts"+i);
			MyBlockPos villageLocation = MyBlockPos.readFromNBT(entry.getCompoundTag("VillageLocation"));
			Set<VillagePost> posts = loadPostSet(entry);
			villagePosts.put(villageLocation, posts);
		}
	}
	
	private Set<VillagePost> loadPostSet(NBTTagCompound compound) {
		Set<VillagePost> ret = new HashSet<>();
		int postCount = compound.getInteger("PostCount");
		for(int i=0; i<postCount; i++){
			ret.add(VillagePost.load(compound.getCompoundTag("Post"+i)));
		}
		return ret;
	}
}
