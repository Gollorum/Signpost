package gollorum.signpost.blocks.tiles;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.blocks.WaystoneContainer;
import gollorum.signpost.event.UpdateWaystoneEvent;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.network.messages.BaseUpdateServerMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.MyBlockPos;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;

public class BasePostTile extends TileEntity implements WaystoneContainer {

	public boolean isCanceled = false;

	public BasePostTile() {}
	
	public BasePostTile setup(){
		SPEventHandler.scheduleTask(() -> {
			if(isCanceled){
				return true;
			}
			if(getWorld()==null){
				return false;
			}
			init();
			return true;
		});
		return this;
	}
	
	public BaseInfo getBaseInfo(){
		return PostHandler.getNativeWaystones().getByPos(toPos());
	}

	public void init(){}

	public MyBlockPos toPos(){
		return new MyBlockPos(pos.getX(), pos.getY(), pos.getZ(), dim());
	}

	public int dim(){
		if(getWorld()==null||getWorld().provider==null){
			return Integer.MIN_VALUE;
		}else
			return getWorld().provider.getDimension();
	}
	
	public void onBlockDestroy(MyBlockPos pos) {
		isCanceled = true;
		BaseInfo base = getBaseInfo();
		if(PostHandler.getNativeWaystones().remove(base)){
			MinecraftForge.EVENT_BUS.post(new UpdateWaystoneEvent(UpdateWaystoneEvent.WaystoneEventType.DESTROYED, getWorld(), this.pos.getX(), this.pos.getY(), this.pos.getZ(), base==null?"":base.getName()));
			NetworkHandler.netWrap.sendToAll(new BaseUpdateClientMessage());
		}
	}

	@Override
	public void setName(String name) {
		BaseInfo ws = getBaseInfo();
		ws.setName(name);
		NetworkHandler.netWrap.sendToServer(new BaseUpdateServerMessage(ws, false));
	}

	@Override
	public String getName() {
		BaseInfo ws = getBaseInfo();
		return ws == null ? "null" : getBaseInfo().toString();
	}

	@Override
	public String toString() {
		return getName();
	}

}
