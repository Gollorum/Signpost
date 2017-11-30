package gollorum.signpost.modIntegration;

import java.util.Set;

import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.collections.Lurchsauna;
import net.blay09.mods.waystones.WaystoneManager;
import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraft.entity.player.EntityPlayer;

public class WaystonesModHandler implements ModHandler {

	@Override
	public Set<BaseInfo> getAllBaseInfos() {
		Lurchsauna<BaseInfo> ret = new Lurchsauna<BaseInfo>();
		for(WaystoneEntry entry: WaystoneManager.getServerWaystones()){
			ret.add(baseInfoFromWaystoneEntry(entry));
		}
		return ret;
	}

	@Override
	public Set<BaseInfo> getAllBaseInfosByPlayer(EntityPlayer player) {
		Lurchsauna<BaseInfo> ret = new Lurchsauna<BaseInfo>();
		for(WaystoneEntry entry: WaystoneManager.getServerWaystones()){
			WaystoneEntry playerEntry = WaystoneManager.getKnownWaystone(entry.getName());
			if(playerEntry != null){
				ret.add(baseInfoFromWaystoneEntry(playerEntry));
			}
		}
		return ret;
	}
	
	private BaseInfo baseInfoFromWaystoneEntry(WaystoneEntry entry){
		if(entry==null){
			return null;
		}
		String name = entry.getName();
		int x = entry.getPos().getX();
		int y = entry.getPos().getY();
		int z = entry.getPos().getZ();
		int dim = entry.getDimensionId();
		return BaseInfo.fromExternal(name, x, y, z, dim, Waystones.MOD_ID);
	}
}