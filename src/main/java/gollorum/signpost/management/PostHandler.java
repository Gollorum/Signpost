package gollorum.signpost.management;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.blocks.tiles.BigPostPostTile;
import gollorum.signpost.blocks.tiles.PostPostTile;
import gollorum.signpost.modIntegration.SignpostAdapter;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.handlers.SendAllWaystoneNamesHandler;
import gollorum.signpost.network.messages.ChatMessage;
import gollorum.signpost.network.messages.TeleportRequestMessage;
import gollorum.signpost.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class PostHandler {

	private static final StonedHashSet allWaystones = new StonedHashSet();
	private static final Map<MyBlockPos, DoubleBaseInfo> posts = new ConcurrentHashMap<>();
	private static final Map<MyBlockPos, BigBaseInfo> bigPosts = new ConcurrentHashMap<>();
	//ServerSide
	private static final Map<UUID, TeleportInformation> awaiting = new ConcurrentHashMap<>();

	/**
	 * UUID = the player;
	 * StringSet = the discovered waystones;
	 */
	static final Map<UUID, StringSet> playerKnownWaystones = new ConcurrentHashMap<>();

	public static StringSet getPlayerKnownWaystoneNames(UUID player){
		return playerKnownWaystones.computeIfAbsent(player, x -> new StringSet());
	}

	static final Map<UUID, PlayerRestrictions> playerKnownWaystonePositions = new ConcurrentHashMap<>();

	public static PlayerRestrictions getPlayerKnownWaystonePositions(UUID player){
		return playerKnownWaystonePositions.computeIfAbsent(player, x -> new PlayerRestrictions());
	}

	public static boolean doesPlayerKnowWaystone(EntityPlayerMP player, BaseInfo waystone){
		if(ClientConfigStorage.INSTANCE.isDisableDiscovery()){
			return true;
		}else{
			return doesPlayerKnowNativeWaystone(player, waystone) || getPlayerKnownWaystones(player).contains(waystone);
		}
	}

	public static boolean doesPlayerKnowNativeWaystone(EntityPlayerMP player, BaseInfo waystone){
		UUID playerID = player.getUniqueID();
		if(ClientConfigStorage.INSTANCE.isDisableDiscovery()){
			return true;
		}else if(getPlayerKnownWaystonePositions(playerID).discoveredWastones.contains(waystone.blockPosition)){
			getPlayerKnownWaystoneNames(playerID).remove(waystone.getName());
			return true;
		}else{
			StringSet known = getPlayerKnownWaystoneNames(playerID);
			if(!known.contains(waystone.getName())) return false;
			known.remove(waystone.getName());
			getPlayerKnownWaystonePositions(playerID).discoveredWastones.add(waystone.blockPosition);
			return true;
		}
	}
	
	public static void init(){
		allWaystones.clear();
		playerKnownWaystones.clear();
		playerKnownWaystonePositions.clear();
		posts.clear();
		bigPosts.clear();
		awaiting.clear();
	}

	public static Map<MyBlockPos, DoubleBaseInfo> getPosts() {
		return posts;
	}

	public static void setPosts(Map<MyBlockPos, DoubleBaseInfo> posts) {
		PostHandler.posts.clear();
		PostHandler.posts.putAll(posts);
		refreshDoublePosts();
	}

	public static Map<MyBlockPos, BigBaseInfo> getBigPosts() {
		return bigPosts;
	}

	public static void setBigPosts(Map<MyBlockPos, BigBaseInfo> bigPosts) {
		PostHandler.bigPosts.clear();
		PostHandler.bigPosts.putAll(bigPosts);
		refreshBigPosts();
	}

	public static List<Sign> getSigns(MyBlockPos pos) {
		List<Sign> ret = new LinkedList();

		DoubleBaseInfo doubleBase = getPosts().get(pos);
		if (doubleBase != null) {
			ret.add(doubleBase.sign1);
			ret.add(doubleBase.sign2);
		} else {
			BigBaseInfo bigBase = getBigPosts().get(pos);
			if (bigBase != null) {
				ret.add(bigBase.sign);
			}
		}

		return ret;
	}

	public static Paintable getPost(MyBlockPos pos) {
		Paintable ret = getPosts().get(pos);
		if (ret == null) {
			ret = getBigPosts().get(pos);
		}
		if (ret == null) {
			pos.getTile();
			ret = getPosts().get(pos);
			if (ret == null) {
				ret = getBigPosts().get(pos);
			}
		}
		return ret;
	}
	
	public static void refreshDoublePosts(){
		for(Entry<MyBlockPos, DoubleBaseInfo> now: posts.entrySet()){
			PostPostTile tile = (PostPostTile) now.getKey().getTile();
			if(tile!=null){
				tile.isWaystone();
				tile.getBases();
			}
		}
	}
	
	public static void refreshBigPosts(){
		for(Entry<MyBlockPos, BigBaseInfo> now: bigPosts.entrySet()){
			BigPostPostTile tile = (BigPostPostTile) now.getKey().getTile();
			if(tile!=null){
				tile.isWaystone();
				tile.getBases();
			}
		}
	}
	
	public static BaseInfo getWSbyName(String name){
		if(ClientConfigStorage.INSTANCE.deactivateTeleportation()){
			return new BaseInfo(name, null, null);
		}else{
			for(BaseInfo now:getAllWaystones()){
				if(name.equals(now.getName())){
					return now;
				}
			}
			return null;
		}
	}

	public static BaseInfo getForceWSbyName(String name){
		if(name==null || name.equals("null")){
			return null;
		}
		for(BaseInfo now:getAllWaystones()){
			if(name.equals(now.getName())){
				return now;
			}
		}
		return new BaseInfo(name, null, null);
	}
	
	public static class TeleportInformation{
		public final BaseInfo destination;
		public final int stackSize;
		public final WorldServer world;
		public final BoolRun boolRun;
		public TeleportInformation(BaseInfo destination, int stackSize, WorldServer world, BoolRun boolRun) {
			this.destination = destination;
			this.stackSize = stackSize;
			this.world = world;
			this.boolRun = boolRun;
		}
	}

	/**
	 * @return whether the player could pay
	 */
	public static boolean pay(EntityPlayer player, BlockPos origin, BlockPos destination){
		if(canPay(player, origin, destination)){
			doPay(player, origin, destination);
			return true;
		}else{
			return false;
		}
	}
	
	public static boolean canPay(EntityPlayer player, BlockPos origin, BlockPos destination){
		if(ClientConfigStorage.INSTANCE.getCost() == null || ConfigHandler.isCreative(player)){
			return true;
		}else{
			Item paymentItem = ClientConfigStorage.INSTANCE.getCost();
			int playerItemCount = 0;
			for(ItemStack now: player.inventory.mainInventory){
				if(now != null && now.getItem() !=null && now.getItem() == paymentItem){
					playerItemCount += now.getCount();
				}
			}
			return playerItemCount >= getStackSize(origin, destination);
		}
	}

	public static void doPay(EntityPlayer player, BlockPos origin, BlockPos destination){
		if(ClientConfigStorage.INSTANCE.getCost() == null || ConfigHandler.isCreative(player)){
			return;
		}else{
			int stackSize = getStackSize(origin, destination);
			player.inventory.clearMatchingItems(ClientConfigStorage.INSTANCE.getCost(), 0, stackSize, null);
		}
	}

	public static int getStackSize(BlockPos origin, BlockPos destination){
		return getStackSize((float) origin.getDistance(destination.getX(), destination.getY(), destination.getZ()));
	}

	public static int getStackSize(MyBlockPos origin, MyBlockPos destination){
		return getStackSize(origin.toBlockPos(), destination.toBlockPos());
	}

	public static int getStackSize(float distance){
		if(ClientConfigStorage.INSTANCE.getCostMult()==0){
			return ClientConfigStorage.INSTANCE.getCostBase();
		}else{
			return (int) (distance / ClientConfigStorage.INSTANCE.getCostMult() + ClientConfigStorage.INSTANCE.getCostBase());
		}
	}

	public static void confirm(final EntityPlayerMP player){
		final TeleportInformation info = awaiting.get(player.getUniqueID());
		SPEventHandler.scheduleTask(() -> {
			if(info==null){
				NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.noConfirm"), player);
				return;
			}else{
				doPay(player, player.getPosition(), info.destination.teleportPosition.toBlockPos());
				SPEventHandler.cancelTask(info.boolRun);
				if(player.dimension != info.destination.teleportPosition.dim){
					player.changeDimension(info.destination.teleportPosition.dim, new SignTeleporter());
				}
				player.setPositionAndUpdate(info.destination.teleportPosition.x+0.5, info.destination.teleportPosition.y+1, info.destination.teleportPosition.z+0.5);
			}
		}, 1);
	}

	public static void teleportMe(BaseInfo destination, final EntityPlayerMP player, int stackSize){
		if(ClientConfigStorage.INSTANCE.deactivateTeleportation()){
			return;
		}
		if(canTeleport(player, destination)){
			WorldServer world = (WorldServer) destination.teleportPosition.getWorld();
			if(world == null){
				NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.errorWorld", "<world>", ""+destination.teleportPosition.dim), player);
			}else{
				UUID uuid = player.getUniqueID();
				TeleportInformation teleportInformation = new TeleportInformation(destination, stackSize, world, new BoolRun(){
					private short ticksLeft = 2400;
					@Override
					public boolean run() {
						if(ticksLeft--<=0){
							awaiting.remove(player.getUniqueID());
							return true;
						}
						return false;
					}
				});
				awaiting.put(uuid, teleportInformation);
				SPEventHandler.scheduleTask(teleportInformation.boolRun);
				NetworkHandler.netWrap.sendTo(new TeleportRequestMessage(stackSize, destination.getName()), player);
			}
		}
	}

	public static boolean addAllDiscoveredByName(UUID player, StringSet ws){
		MyBlockPosSet set = new MyBlockPosSet();
		StringSet newStrs = new StringSet();
		newStrs.addAll(ws);
		for(String now: ws){
			for(BaseInfo base: getAllWaystones()){
				if(base.getName().equals(now)){
					set.add(base.blockPosition);
					newStrs.remove(now);
				}
			}
		}
		ws = newStrs;
		boolean ret = false;
		if(!ws.isEmpty()) {
			ret = getPlayerKnownWaystoneNames(player).addAll(ws);
		}
		return ret | getPlayerKnownWaystonePositions(player).discoveredWastones.addAll(set);
	}
	
	public static boolean addAllDiscoveredByPos(UUID player, MyBlockPosSet ws){
		return getPlayerKnownWaystonePositions(player).discoveredWastones.addAll(ws);
	}
	
	public static boolean addDiscovered(UUID player, BaseInfo ws){
		if(ws==null){
			return false;
		}
		boolean ret = getPlayerKnownWaystonePositions(player).discoveredWastones.add(ws.blockPosition);
		ret = ret |! getPlayerKnownWaystoneNames(player).remove(ws.getName());
		return ret;
	}
	
	public static void refreshDiscovered(){
		HashSet<UUID> toDelete = new HashSet<UUID>();
		HashMap<UUID, MyBlockPosSet> toAdd = new HashMap<UUID, MyBlockPosSet>();
		for(Entry<UUID, StringSet> now: playerKnownWaystones.entrySet()){
			StringSet newSet = new StringSet();
			MyBlockPosSet newPosSet = new MyBlockPosSet();
			for(String str: now.getValue()){
				for(BaseInfo base: allWaystones){
					if(base.hasName() && base.getName().equals(str)){
						newPosSet.add(base.blockPosition);
						newSet.add(str);
					}
				}
			}
			toAdd.put(now.getKey(), newPosSet);
			now.getValue().removeAll(newSet);
			if(now.getValue().isEmpty()){
				toDelete.add(now.getKey());
			}
		}
		
		for(UUID now: toDelete){
			playerKnownWaystones.remove(now);
		}
		
		for(Entry<UUID, MyBlockPosSet> now: toAdd.entrySet()){
			addAllDiscoveredByPos(now.getKey(), now.getValue());
		}
	}
	
	public static boolean canTeleport(EntityPlayerMP player, BaseInfo target){
		if(doesPlayerKnowWaystone(player, target)){
			if(new MyBlockPos(player).checkInterdimensional(target.blockPosition)){
				return true;
			}else{
				NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.guiWorldDim"), player);
			}
		}else{
			NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.notDiscovered", "<Waystone>", target.getName()), player);
		}
		return false;
	}

	public static EntityPlayer getPlayerByName(String name){
		return FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUsername(name);
	}
	
	public static boolean isHandEmpty(EntityPlayer player){
		return player.getHeldItemMainhand().getItem().equals(Item.getItemFromBlock(Blocks.AIR));
	}

	private static class SignTeleporter implements ITeleporter {

		@Override
		public void placeEntity(World world, Entity entity, float yaw) {}
	}

	public static StonedHashSet getAllWaystones() {
		StonedHashSet ret = SignpostAdapter.INSTANCE.getExternalBaseInfos();
		ret.addAll(allWaystones);
		return ret;
	}
	
	public static Collection<String> getAllWaystoneNames(){
		Collection<String> ret = getAllWaystones().select(b -> b.getName());
		if(FMLCommonHandler.instance().getEffectiveSide().equals(Side.CLIENT)) {
			ret.addAll(SendAllWaystoneNamesHandler.cachedWaystoneNames);
		}
		return ret;
	}

	public static StonedHashSet getNativeWaystones(){
		return allWaystones;
	}

	public static void setNativeWaystones(StonedHashSet set){
		allWaystones.clear();
		allWaystones.addAll(set);
	}

	public static StonedHashSet getPlayerKnownWaystones(EntityPlayerMP player){
		StonedHashSet ret = SignpostAdapter.INSTANCE.getExternalPlayerBaseInfos(player);
		for(BaseInfo now: allWaystones){
			if(doesPlayerKnowNativeWaystone(player, now)){
				ret.add(now);
			}
		}
		return ret;
	}

	public static boolean addWaystone(BaseInfo baseInfo){
		return allWaystones.add(baseInfo);
	}

}
