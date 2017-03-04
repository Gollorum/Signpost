package gollorum.signpost.blocks;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.network.messages.BaseUpdateServerMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.MyBlockPos;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class BasePostTile extends TileEntity {

	public BaseInfo ws;

	public BasePostTile() {
		SPEventHandler.scheduleTask(new Runnable() {
			@Override
			public void run() {
				boolean found = false;
				for (BaseInfo now : PostHandler.allWaystones) {
					if (now.sameAs(new BaseInfo(null, new MyBlockPos("", pos, world.provider.getDimension()), null))) {
						ws = now;
						found = true;
						break;
					}
				}
				if (!found) {
					ws = new BaseInfo(null, new MyBlockPos("", pos, world.provider.getDimension()), null);
					PostHandler.allWaystones.add(ws);
				}
			}
		}, 20);
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
