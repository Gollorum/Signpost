package gollorum.signpost.worldGen.villages;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import gollorum.signpost.blocks.SuperPostPost;
import gollorum.signpost.management.ClientConfigStorage;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.Paintable;
import gollorum.signpost.util.Sign;
import net.minecraft.world.biome.Biome;

class LibrarySignpostHelper extends LibraryHelper {
	private MyBlockPos signpostLocation;
	private Map<MyBlockPos, MyBlockPos> villageWaystones;

	LibrarySignpostHelper(MyBlockPos villageLocation, MyBlockPos signpostLocation, Map<MyBlockPos, MyBlockPos> villageWaystones) {
		super(villageLocation);
		this.signpostLocation = signpostLocation;
		this.villageWaystones = villageWaystones;
	}

	void enscribeNewSign(double optimalRot) {
		List<Sign> signs = PostHandler.getSigns(signpostLocation);
		Paintable post = PostHandler.getPost(signpostLocation);
		Biome biome = signpostLocation.getBiome();
		if(!(post == null || biome == null)){
			post.setTextureToBiomeDefault(biome);
		}
		post = PostHandler.getPost(signpostLocation.getBelow());
		if(!(post == null || biome == null)){
			post.setTextureToBiomeDefault(biome);
		}
		List<MyBlockPos> closestWaystones = getClosestWaystones(signs.size());
		for(int i=0; i<signs.size() && i<closestWaystones.size(); i++){
			if(signs.get(i).base != null){
				continue;
			}
			signs.get(i).base = getBaseInfo(closestWaystones.get(i));
			signs.get(i).point = true;
			if(angleTooLarge(calcRot(signpostLocation, closestWaystones.get(i)), optimalRot)){
				signs.get(i).flip = true;
			}
			if(biome != null){
				signs.get(i).setTextureToBiomeDefault(biome);
			}
		}
		SuperPostPost.updateServer(signpostLocation);
	}

	private List<MyBlockPos> getClosestWaystones(int size) {
		List<MyBlockPos> waystones = getAllowedWaystones();
		waystones.sort(new Comparator<MyBlockPos>(){
			@Override
			public int compare(MyBlockPos pos1, MyBlockPos pos2) {
				return compareClosest(pos1, pos2, signpostLocation);
			}
		});
		MyBlockPos myPos = this.villageWaystones.get(villageLocation);
		for(MyBlockPos now: waystones){
			if(now.equals(myPos)){
				myPos = now;
				break;
			}
		}
		waystones.remove(myPos);
		return waystones;
	}
	
	private List<MyBlockPos> getAllowedWaystones() {
		List<MyBlockPos> ret = new LinkedList<MyBlockPos>();
		if(ClientConfigStorage.INSTANCE.isOnlyVillageTargets()){
			ret.addAll(villageWaystones.values());
		}else{
			ret.addAll(PostHandler.getNativeWaystones().positions());
		}
		return ret;
	}
}
