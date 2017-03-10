package gollorum.signpost.blocks;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.network.messages.BaseUpdateServerMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.BoolRun;
import gollorum.signpost.util.MyBlockPos;
import net.minecraft.tileentity.TileEntity;

public class BasePostTile extends TileEntity {

	public boolean isCanceled = false;

	public BasePostTile() {}
	
	public BasePostTile setup(){
		SPEventHandler.scheduleTask(new BoolRun() {
			@Override
			public boolean run() {
				if(isCanceled){
					return true;
				}
				if(worldObj==null){
					return false;
				}
				init();
				return true;
			}
		});
		return this;
	}

	public BaseInfo getBaseInfo(){
		return PostHandler.allWaystones.getByPos(toPos());
	}
	
	public void init(){
		boolean found = false;
		if(getBaseInfo()!=null){
			return;
		}
		PostHandler.allWaystones.add(new BaseInfo(null, toPos(), null));
	}

	public MyBlockPos toPos(){
		if(worldObj==null||worldObj.isRemote){
			return new MyBlockPos("", pos.getX(), pos.getY(), pos.getZ(), dim());
		}else{
			return new MyBlockPos(worldObj.getWorldInfo().getWorldName(), pos.getX(), pos.getY(), pos.getZ(), dim());
		}
	}

	public int dim(){
		if(worldObj==null||worldObj.provider==null){
			return Integer.MIN_VALUE;
		}else
			return worldObj.provider.getDimension();
	}
	
	public void onBlockDestroy(MyBlockPos pos) {
		isCanceled = true;
		if(PostHandler.allWaystones.removeByPos(pos)){
			NetworkHandler.netWrap.sendToAll(new BaseUpdateClientMessage());
		}
	}

	public void setName(String name) {
		BaseInfo ws = getBaseInfo();
		ws.name = name;
		NetworkHandler.netWrap.sendToServer(new BaseUpdateServerMessage(ws, false));
	}

	public String getName() {
		BaseInfo ws = getBaseInfo();
		return ws == null ? "null" : ws.toString();
	}

	@Override
	public String toString() {
		return "wsn:" + getName();
	}

}
