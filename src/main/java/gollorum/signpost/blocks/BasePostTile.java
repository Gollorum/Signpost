package gollorum.signpost.blocks;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.network.messages.BaseUpdateServerMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.BlockPos;
import net.minecraft.tileentity.TileEntity;

public class BasePostTile extends TileEntity {

	public BaseInfo ws;

	public BasePostTile() {
		SPEventHandler.scheduleTask(new Runnable() {
			@Override
			public void run() {
				boolean found = false;
				for (BaseInfo now : PostHandler.allWaystones) {
					if (now.sameAs(new BaseInfo(null, new BlockPos("", xCoord, yCoord, zCoord, worldObj.provider.dimensionId), null))) {
						ws = now;
						found = true;
						break;
					}
				}
				if (!found) {
					System.out.println("ERROR: Waystone not found!");
					ws = new BaseInfo(null, new BlockPos("", xCoord, yCoord, zCoord, worldObj.provider.dimensionId), null);
					PostHandler.allWaystones.add(ws);
				}
			}
		}, 20);
	}

	public void onBlockDestroy() {
		if(PostHandler.allWaystones.remove(ws)){
			NetworkHandler.netWrap.sendToAll(new BaseUpdateClientMessage().init());
		}
	}

	public void setName(String name) {
		ws.name = name;
		NetworkHandler.netWrap.sendToServer(new BaseUpdateServerMessage(ws, false));
	}

	public String getName() {
		return ws == null ? "null" : ws.toString();
	}

	@Override
	public String toString() {
		return "wsn:" + getName();
	}
}
