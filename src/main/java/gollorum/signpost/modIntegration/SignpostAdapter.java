package gollorum.signpost.modIntegration;

import cpw.mods.fml.common.Loader;
import gollorum.signpost.util.StonedHashSet;
import net.minecraft.entity.player.EntityPlayer;

import java.util.HashSet;
import java.util.Set;

public class SignpostAdapter {
	
	public static final SignpostAdapter INSTANCE = new SignpostAdapter();
	
	public static final String WAYSTONES_MOD_ID = "waystones";
	
	private final Set<ModHandler> handlers;
	
	private SignpostAdapter(){
		handlers = new HashSet<>();
				
		if(Loader.isModLoaded(WAYSTONES_MOD_ID)){
			registerModHandler(new WaystonesModHandler());
		}
	}
	
	public boolean registerModHandler(ModHandler handler){
		return handlers.add(handler);
	}
	
	public boolean removeModHandler(ModHandler handler){
		return handlers.remove(handler);
	}

	public StonedHashSet getExternalBaseInfos(){
		StonedHashSet ret = new StonedHashSet();
		for(ModHandler now: handlers){
			ret.addAll(now.getAllBaseInfos());
		}
		return ret;
	}
	
	public StonedHashSet getExternalPlayerBaseInfos(EntityPlayer player){
		StonedHashSet ret = new StonedHashSet();
		for(ModHandler now: handlers){
			ret.addAll(now.getAllBaseInfosByPlayer(player));
		}
		return ret;
	}
}
