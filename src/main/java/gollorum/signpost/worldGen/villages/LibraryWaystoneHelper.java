package gollorum.signpost.worldGen.villages;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.blocks.SuperPostPost;
import gollorum.signpost.blocks.tiles.SuperPostPostTile;
import gollorum.signpost.util.BoolRun;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.Sign;
import gollorum.signpost.util.code.MinecraftIndependent;

import java.util.*;
import java.util.Map.Entry;

@MinecraftIndependent
class LibraryWaystoneHelper extends LibraryHelper {
	private MyBlockPos waystoneLocation;
	private Map<MyBlockPos, Set<VillagePost>> villagePosts;

	LibraryWaystoneHelper(MyBlockPos villageLocation, Map<MyBlockPos, Set<VillagePost>> villagePosts,
			MyBlockPos waystoneLocation) {
		super(villageLocation);
		this.villagePosts = villagePosts;
		this.waystoneLocation = waystoneLocation;
	}

	void enscribeEmptySign() {
		SPEventHandler.scheduleTask(new BoolRun() {
			boolean go = false;
			@Override
			public boolean run() {
				if (!go) {
					go = true;
					return false;
				}
				try {
					final Set<VillagePost> posts = fetchOtherVillagesPosts();
					final SignChooser.SingeEmptySign emptySign = new SignChooser(posts).getBestSign();
					if (emptySign != null) {
						final MyBlockPos pos = emptySign.post.getTopSignPosition();
						final Sign sign = SuperPostPost.getSuperTile(pos).getEmptySigns().get(0);
						if (pos.getTile() instanceof SuperPostPostTile && ((SuperPostPostTile) pos.getTile()).isLoading()) {
							return false;
						} else {
							sign.base = getBaseInfo(waystoneLocation);
							sign.point = true;
							if (angleTooLarge(calcRot(pos, waystoneLocation), emptySign.post.desiredRotation)) {
								emptySign.sign.flip = true;
							}
							SuperPostPost.updateServer(pos);
							return true;
						}
					} else {
						return true;
					}
				} catch(Exception e) {return true;}
			}
		});
	}

	private Set<VillagePost> fetchOtherVillagesPosts() {
		Set<VillagePost> posts = new HashSet<>();
		for (Entry<MyBlockPos, Set<VillagePost>> entry : villagePosts.entrySet()) {
			if (!(entry.getKey().equals(villageLocation) || containsMeAsTarget(entry.getValue()))) {
				posts.addAll(entry.getValue());
			}
		}
		return posts;
	}

	private boolean containsMeAsTarget(Set<VillagePost> posts) {
		for(VillagePost post : posts) {
			for(Sign sign : post.getSigns()) {
				if(sign != null && sign.isValid() && sign.base != null && sign.base.blockPosition != null && sign.base.blockPosition.equals(waystoneLocation)){
					return true;
				}
			}
		}
		return false;
	}

	private class SignChooser {
		private List<VillageSign> signs = new LinkedList();

		public SignChooser(Set<VillagePost> posts) {
			fetchSigns(posts);
		}

		public SingeEmptySign getBestSign() {
			sort();
			if (signs.size() > 0) {
				for(VillageSign sign : signs) {
					if(sign == null || sign.post == null) continue;
					List<Sign> postSigns = sign.signs;
					if(postSigns.size() == 0) continue;
					final MyBlockPos pos = sign.post.getTopSignPosition();
					if (pos != null && SuperPostPost.getSuperTile(pos) != null) {
						try {
							SuperPostPost.getSuperTile(pos).getEmptySigns().get(0);
							return new SingeEmptySign(postSigns.get(0), sign.post);
						} catch(Exception e) {}
					}
				}
			}
			return null;
		}

		private void fetchSigns(Set<VillagePost> posts) {
			for (VillagePost post : posts) {
				List<Sign> signs = getEmptySigns(post);
				if (!signs.isEmpty()) {
					this.signs.add(new VillageSign(new VillagePost(post.getTopSignPosition(), post.desiredRotation), signs));
				}
			}
		}

		private void sort() {
			sortDistance();
			sortSignCount();
		}

		private void sortDistance() {
			signs.sort(new Comparator<VillageSign>() {
				@Override
				public int compare(VillageSign sign1, VillageSign sign2) {
					return compareClosest(sign1.post.getTopSignPosition(), sign2.post.getTopSignPosition(), villageLocation);
				}
			});
		}

		private void sortSignCount() {
			signs.sort(new Comparator<VillageSign>() {
				@Override
				public int compare(VillageSign sign1, VillageSign sign2) {
					return new Integer(sign2.signs.size()).compareTo(sign1.signs.size());
				}
			});
		}

		private List<Sign> getEmptySigns(VillagePost post) {
			List<Sign> signs = post.getSigns();
			List<Sign> ret = new LinkedList();
			for (Sign now : signs) {
				if (now.base == null || !now.base.hasName()) {
					ret.add(now);
				}
			}
			return ret;
		}

		private class VillageSign {
			public VillagePost post;
			public List<Sign> signs;

			public VillageSign(VillagePost post, List<Sign> signs) {
				this.post = post;
				this.signs = signs;
			}
		}

		public class SingeEmptySign {
			public Sign sign;
			public VillagePost post;

			public SingeEmptySign(Sign sign, VillagePost post) {
				this.sign = sign;
				this.post = post;
			}
		}
	}
}
