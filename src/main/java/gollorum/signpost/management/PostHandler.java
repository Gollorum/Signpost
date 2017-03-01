package gollorum.signpost.management;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cpw.mods.fml.common.FMLCommonHandler;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.BlockPos;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.StonedHashSet;
import gollorum.signpost.util.StringSet;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

public class PostHandler {

	public static StonedHashSet allWaystones = new StonedHashSet();
	public static HashMap<UUID, StringSet> playerKnownWaystones = new HashMap<UUID, StringSet>();
	public static PostMap posts = new PostMap();	
	
	public static class PostMap extends HashMap<BlockPos, DoubleBaseInfo>{
		@Override
		public DoubleBaseInfo remove(Object key){
			if(key instanceof BlockPos){
				BlockPos k = (BlockPos) key;
				for(Entry<BlockPos, DoubleBaseInfo> now: this.entrySet()){
					if(now.getKey().equals(k)){
						key = now.getKey();
						break;
					}
				}
			}
			return super.remove(key);
		}
	}
	
	public static BaseInfo getWSbyName(String name){
		for(BaseInfo now:allWaystones){
			if(now.name.equals(name)){
				return now;
			}
		}
		return null;
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
					return(now.getValue().remove(newWS));
				}
			}
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
	
}
