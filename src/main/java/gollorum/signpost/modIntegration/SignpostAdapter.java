package gollorum.signpost.modIntegration;

import java.util.Set;

import cpw.mods.fml.common.Loader;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.collections.Lurchsauna;
import net.minecraft.entity.player.EntityPlayer;

public class SignpostAdapter {
	
	public static final SignpostAdapter INSTANCE = new SignpostAdapter();
	
	private final String WAYSTONES_MOD_ID = "waystones";
	
	private final Set<ModHandler> handlers;
	
	private SignpostAdapter(){
		handlers = new Lurchsauna<ModHandler>();
				
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

	public Set<BaseInfo> getExternalBaseInfos(){
		Set<BaseInfo> ret = new Lurchsauna<BaseInfo>();
		for(ModHandler now: handlers){
			ret.addAll(now.getAllBaseInfos());
		}
		return ret;
	}
	
	public Set<BaseInfo> getExternalPlayerBaseInfos(EntityPlayer player){
		Set<BaseInfo> ret = new Lurchsauna<BaseInfo>();
		for(ModHandler now: handlers){
			ret.addAll(now.getAllBaseInfosByPlayer(player));
		}
		return ret;
	}
}
