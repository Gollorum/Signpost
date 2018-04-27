package gollorum.signpost.worldGen.villages;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.blocks.SuperPostPost;
import gollorum.signpost.blocks.tiles.SuperPostPostTile;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.util.BoolRun;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.Sign;
import gollorum.signpost.util.collections.Lurchsauna;
import net.minecraft.tileentity.TileEntity;

class LibraryWaystoneHelper extends LibraryHelper {
	private MyBlockPos waystoneLocation;
	private Map<MyBlockPos, Set<VillagePost>> villagePosts;

	LibraryWaystoneHelper(MyBlockPos villageLocation, Map<MyBlockPos, Set<VillagePost>> villagePosts, MyBlockPos waystoneLocation) {
		super(villageLocation);
		this.villagePosts = villagePosts;
		this.waystoneLocation = waystoneLocation;
	}

	void enscribeEmptySign(){
		final Set<VillagePost> posts = fetchOtherVillagesPosts();
		final SignChooser.SingeEmptySign emptySign = new SignChooser(posts).getBestSign();
		if(emptySign != null){
			final MyBlockPos pos = emptySign.post.topSignPosition;
			final Sign sign = SuperPostPost.getSuperTile(pos).getEmptySigns().get(0);
			SPEventHandler.scheduleTask(new BoolRun(){
				boolean go = false;
				@Override
				public boolean run() {
					TileEntity tile = pos.getTile();
					if(tile instanceof SuperPostPostTile && ((SuperPostPostTile)tile).isLoading()){
						return false;
					}
					if(!go){
						go = true;
						return false;
					}else{
						sign.base = getBaseInfo(waystoneLocation);
						sign.point = true;
						if(angleTooLarge(calcRot(pos, waystoneLocation), emptySign.post.desiredRotation)){
							emptySign.sign.flip = true;
						}
						SuperPostPost.updateServer(pos);
						return true;
					}
				}
				
			});
		}
	}

	private Set<VillagePost> fetchOtherVillagesPosts() {
		Set<VillagePost> posts = new Lurchsauna();
		for(Entry<MyBlockPos, Set<VillagePost>> entry: villagePosts.entrySet()){
			if(!entry.getKey().equals(villageLocation)){
				posts.addAll(entry.getValue());
			}
		}
		return posts;
	}
	
	private class SignChooser{
		private List<VillageSign> signs = new LinkedList();
		
		public SignChooser(Set<VillagePost> posts){
			fetchSigns(posts);
		}
		
		public SingeEmptySign getBestSign(){
			sort();
			if(signs.size() > 0){
				List<Sign> postSigns = signs.get(0).signs;
				if(postSigns.size() > 0){
					return new SingeEmptySign(postSigns.get(0), signs.get(0).post);
				}
			}
			return null;
		}
		
		private void fetchSigns(Set<VillagePost> posts) {
			for(VillagePost post: posts){
				List<Sign> signs = getEmptySigns(post.topSignPosition);
				if(!signs.isEmpty()){
					this.signs.add(new VillageSign(new VillagePost(post.topSignPosition, post.desiredRotation), signs));
				}
			}
		}

		private void sort(){
			sortDistance();
			sortSignCount();
		}
		
		private void sortDistance(){
			signs.sort(new Comparator<VillageSign>() {
				@Override
				public int compare(VillageSign sign1, VillageSign sign2) {
					return compareClosest(sign1.post.topSignPosition, sign2.post.topSignPosition, villageLocation);
				}
			});
		}
		
		private void sortSignCount(){
			signs.sort(new Comparator<VillageSign>() {
				@Override
				public int compare(VillageSign sign1, VillageSign sign2) {
					return new Integer(sign2.signs.size()).compareTo(sign1.signs.size());
				}
			});
		}
		
		private List<Sign> getEmptySigns(MyBlockPos postLocation){
			List<Sign> signs = PostHandler.getSigns(postLocation);
			List<Sign> ret = new LinkedList();
			for(Sign now: signs){
				if(now.base == null ||! now.base.hasName()){
					ret.add(now);
				}
			}
			return ret;
		}
		
		private class VillageSign{
			public VillagePost post;
			public List<Sign> signs;
			public VillageSign(VillagePost post, List<Sign> signs) {
				this.post = post;
				this.signs = signs;
			}
		}
		
		public class SingeEmptySign{
			public Sign sign;
			public VillagePost post;
			public SingeEmptySign(Sign sign, VillagePost post) {
				this.sign = sign;
				this.post = post;
			}
		}
	}
}
