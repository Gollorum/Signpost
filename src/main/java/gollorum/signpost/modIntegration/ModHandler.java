package gollorum.signpost.modIntegration;

import java.util.Set;

import gollorum.signpost.util.BaseInfo;
import net.minecraft.entity.player.EntityPlayer;

public interface ModHandler {

	/**
	 * use BaseInfo.fromExternal()
	 */
	Set<BaseInfo> getAllBaseInfos();
	Set<BaseInfo> getAllBaseInfosByPlayer(EntityPlayer player);
	
}
