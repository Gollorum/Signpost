package gollorum.signpost.management;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.ChatMessage;
import gollorum.signpost.network.messages.SendAllPostBasesMessage.DoubleStringInt;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.StonedHashSet;
import gollorum.signpost.util.StringSet;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class PostHandler {

	public static StonedHashSet allWaystones;
	public static HashMap<UUID, StringSet> playerKnownWaystones;
	public static PostMap posts;	

	public static void preinit() {
		allWaystones = new StonedHashSet();
		playerKnownWaystones = new HashMap<UUID, StringSet>();
		posts = new PostMap();	
	}
	
	public static class PostMap extends HashMap<MyBlockPos, DoubleBaseInfo>{
		@Override
		public DoubleBaseInfo remove(Object key){
			if(key instanceof MyBlockPos){
				MyBlockPos k = (MyBlockPos) key;
				for(Entry<MyBlockPos, DoubleBaseInfo> now: this.entrySet()){
					if(now.getKey().equals(k)){
						key = now.getKey();
						break;
					}
				}
			}
			return super.remove(key);
		}

		public DoubleBaseInfo get(Object obj){
			for(Entry<MyBlockPos, DoubleBaseInfo> now: this.entrySet()){
				if(now.getKey().equals(obj)){
					return now.getValue();
				}
			}
			return null;
		}
		
		public void keepSame(HashMap<MyBlockPos, DoubleStringInt> posts) {
			HashSet<MyBlockPos> toDelete = new HashSet<MyBlockPos>();
			toDelete.addAll(this.keySet());
			for(Entry<MyBlockPos, DoubleStringInt> now: posts.entrySet()){
				for(Entry<MyBlockPos, DoubleBaseInfo> now2: this.entrySet()){
					if(now.getKey().equals(now2.getKey())){
						toDelete.remove(now2.getKey());
					}
				}
			}
			for(MyBlockPos now: toDelete){
				this.remove(now);
			}
		}
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

	public static void teleportMe(BaseInfo destination, EntityPlayerMP player, int stackSize){
		if(ConfigHandler.deactivateTeleportation){
			return;
		}
		if(canTeleport(player, destination)){
			World world = PostHandler.getWorldByName(destination.pos.world);
			if(world == null){
				NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.errorWorld", "<world>", destination.pos.world), player);
			}else{
				player.inventory.clearMatchingItems(ConfigHandler.cost, 0, stackSize, null);
				if(!player.world.equals(world)){
					player.setWorld(world);
				}
				if(!(player.dimension==destination.pos.dim)){
					player.changeDimension(destination.pos.dim);
				}
				player.setPositionAndUpdate(destination.pos.x+0.5, destination.pos.y+1, destination.pos.z+0.5);
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
		for(World now: FMLCommonHandler.instance().getMinecraftServerInstance().worlds){
			if(now.getWorldInfo().getWorldName().equals(world)){
				return now;
			}
		}
		return null;
	}

}
