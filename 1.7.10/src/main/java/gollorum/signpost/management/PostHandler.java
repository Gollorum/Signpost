package gollorum.signpost.management;

import java.util.Map;
import java.util.UUID;

import cpw.mods.fml.common.FMLCommonHandler;
import gollorum.signpost.SPEventHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.ChatMessage;
import gollorum.signpost.network.messages.TeleportRequestMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.BigBaseInfo;
import gollorum.signpost.util.BlockPos;
import gollorum.signpost.util.BoolRun;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.StonedHashSet;
import gollorum.signpost.util.StringSet;
import gollorum.signpost.util.collections.Lurchpaerchensauna;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class PostHandler {

	public static StonedHashSet allWaystones = new StonedHashSet();
	public static Lurchpaerchensauna<BlockPos, DoubleBaseInfo> posts = new Lurchpaerchensauna<BlockPos, DoubleBaseInfo>();
	public static Lurchpaerchensauna<BlockPos, BigBaseInfo> bigPosts = new Lurchpaerchensauna<BlockPos, BigBaseInfo>();
	//ServerSide
	public static Lurchpaerchensauna<UUID, StringSet> playerKnownWaystones = new Lurchpaerchensauna<UUID, StringSet>();
	public static Lurchpaerchensauna<UUID, TeleportInformation> awaiting =  new Lurchpaerchensauna<UUID, TeleportInformation>(); 
	
	public static void init(){
		allWaystones = new StonedHashSet();
		playerKnownWaystones = new Lurchpaerchensauna<UUID, StringSet>();
		posts = new Lurchpaerchensauna<BlockPos, DoubleBaseInfo>();
		bigPosts = new Lurchpaerchensauna<BlockPos, BigBaseInfo>();
	}
	
	public static BaseInfo getWSbyName(String name){
		if(ConfigHandler.deactivateTeleportation){
			return new BaseInfo(name, null, null);
		}else{
			for(BaseInfo now:allWaystones){
				if(name.equals(now.name)){
					return now;
				}
			}
			return null;
		}
	}
	
	public static class TeleportInformation{
		public BaseInfo destination;
		public int stackSize;
		public World world;
		public BoolRun boolRun;
		public TeleportInformation(BaseInfo destination, int stackSize, World world, BoolRun boolRun) {
			this.destination = destination;
			this.stackSize = stackSize;
			this.world = world;
			this.boolRun = boolRun;
		}
	}
	
	public static boolean pay(EntityPlayer player, int x1, int y1, int z1, int x2, int y2, int z2){
		if(canPay(player, x1, y1, 1, x2, y2, z2)){
			doPay(player, x1, y1, z1, x2, y2, z2);
			return true;
		}else{
			return false;
		}
	}
	
	public static boolean canPay(EntityPlayer player, int x1, int y1, int z1, int x2, int y2, int z2){
		if(ConfigHandler.cost == null){
			return true;
		}else{
			int playerItemCount = 0;
			for(ItemStack now: player.inventory.mainInventory){
				if(now != null && now.getItem() !=null && now.getItem().getClass() == ConfigHandler.cost.getClass()){
					playerItemCount += now.stackSize;
				}
			}
			int dx = x1-x2; int dy = y1-y2; int dz = z1-z2;
			int stackSize = (int) Math.sqrt(dx*dx+dy*dy+dz*dz) / ConfigHandler.costMult + 1;
			return playerItemCount>=stackSize;
		}
	}
	
	public static void doPay(EntityPlayer player, int x1, int y1, int z1, int x2, int y2, int z2){
		if(ConfigHandler.cost == null){
			return;
		}else{
			int dx = x1-x2; int dy = y1-y2; int dz = z1-z2;
			int stackSize = (int) Math.sqrt(dx*dx+dy*dy+dz*dz) / ConfigHandler.costMult + 1;
			while(stackSize-->0){
				player.inventory.consumeInventoryItem(ConfigHandler.cost);
			}
		}
	}
	
	public static void confirm(EntityPlayerMP player){
		TeleportInformation info = awaiting.get(player.getUniqueID());
		if(info==null){
			NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.noConfirm"), player);
			return;
		}else{
			SPEventHandler.cancelTask(awaiting.remove(player.getUniqueID()).boolRun);
			doPay(player, 0, 0, 0, 0, 0, (info.stackSize-1)*ConfigHandler.costMult);
//			if(ConfigHandler.cost!=null){
//				for(int i=0; i<info.stackSize; i++){
//					player.inventory.consumeInventoryItem(ConfigHandler.cost);
//				}
//			}
			ServerConfigurationManager manager = FMLCommonHandler.instance().getMinecraftServerInstance().getConfigurationManager();
			if(!player.worldObj.equals(info.world)){
				manager.transferEntityToWorld(player, 1, (WorldServer)player.worldObj, (WorldServer)info.world);
			}
			if(!(player.dimension==info.destination.pos.dim)){
				manager.transferPlayerToDimension(player, info.destination.pos.dim);
			}
			player.setPositionAndUpdate(info.destination.pos.x+0.5, info.destination.pos.y+1, info.destination.pos.z+0.5);
		}
	}

	public static void teleportMe(BaseInfo destination, final EntityPlayerMP player, int stackSize){
		if(ConfigHandler.deactivateTeleportation){
			return;
		}
		if(canTeleport(player, destination)){
			World world = PostHandler.getWorldByName(destination.pos.world);
			if(world == null){
				NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.errorWorld", "<world>", destination.pos.world), player);
			}else{
				SPEventHandler.scheduleTask(awaiting.put(player.getUniqueID(), new TeleportInformation(destination, stackSize, world, new BoolRun(){
					private short ticksLeft = 2400;
					@Override
					public boolean run() {
						if(ticksLeft--<=0){
							awaiting.remove(player.getUniqueID());
							return true;
						}
						return false;
					}
				})).boolRun);
				NetworkHandler.netWrap.sendTo(new TeleportRequestMessage(stackSize, destination.name), player);
			}
		}else{
			NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.notDiscovered", "<Waystone>", destination.name), player);
		}
	}
	
	public static StonedHashSet getByWorld(String world){
		StonedHashSet ret = new StonedHashSet();
		for(BaseInfo now: allWaystones){
			if(now.pos.world.equals(world)){
				ret.add(now);
			}
		}
		return ret;
	}
	
	public static boolean updateWS(BaseInfo newWS, boolean destroyed){
		if(destroyed){
			if(allWaystones.remove(getWSbyName(newWS.name))){
				for(Map.Entry<UUID, StringSet> now: playerKnownWaystones.entrySet()){
					now.getValue().remove(newWS);
				}
				return true;
			}
			return false;
		}
		for(BaseInfo now: allWaystones){
			if(now.update(newWS)){
				return true;
			}
		}
		return allWaystones.add(newWS);
	}
	
	public static boolean addAllDiscoveredByName(UUID player, StringSet ws){
		if(playerKnownWaystones.containsKey(player)){
			return playerKnownWaystones.get(player).addAll(ws);
		}else{
			StringSet newSet = new StringSet();
			boolean ret = newSet.addAll(ws);
			playerKnownWaystones.put(player, newSet);
			return ret;
		}
	}
	
	public static boolean addDiscovered(UUID player, BaseInfo ws){
		if(ws==null){
			return false;
		}
		if(playerKnownWaystones.containsKey(player)){
			return playerKnownWaystones.get(player).add(ws+"");
		}else{
			StringSet newSet = new StringSet();
			newSet.add(""+ws);
			playerKnownWaystones.put(player, newSet);
			return true;
		}
	}
	
	public static boolean canTeleport(EntityPlayerMP player, BaseInfo target){
		StringSet playerKnows = PostHandler.playerKnownWaystones.get(player.getUniqueID());
		if(playerKnows==null){
			return false;
		}
		return playerKnows.contains(target.name);
	}
	
	public static World getWorldByName(String world){
		for(World now: FMLCommonHandler.instance().getMinecraftServerInstance().worldServers){
			if(now.getWorldInfo().getWorldName().equals(world)){
				return now;
			}
		}
		return null;
	}

	public static boolean addRep(BaseInfo ws) {
		BaseInfo toDelete = allWaystones.getByPos(ws.pos);
		allWaystones.removeByPos(toDelete.pos);
		allWaystones.add(ws);
		return true;
	}
}
