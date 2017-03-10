package gollorum.signpost.blocks;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.network.messages.BaseUpdateServerMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.BlockPos;
import gollorum.signpost.util.BoolRun;
import net.minecraft.tileentity.TileEntity;

public class BasePostTile extends TileEntity {

//	public BaseInfo ws;
	public boolean isCanceled = false;

	public BasePostTile() {
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
	}
	
	public BaseInfo getBaseInfo(){
		return PostHandler.allWaystones.getByPos(toPos());
	}

	public void init(){
		boolean found = false;
		if(getBaseInfo()!=null){
			return;
		}
		BaseInfo ws = new BaseInfo(null, toPos(), null);
		PostHandler.allWaystones.add(ws);
	}

	public BlockPos toPos(){
		if(worldObj==null||worldObj.isRemote){
			return new BlockPos("", xCoord, yCoord, zCoord, dim());
		}else{
			return new BlockPos(worldObj.getWorldInfo().getWorldName(), xCoord, yCoord, zCoord, dim());
		}
	}

	public int dim(){
		if(worldObj==null||worldObj.provider==null){
			return Integer.MIN_VALUE;
		}else
			return worldObj.provider.dimensionId;
	}
	
	public void onBlockDestroy(BlockPos pos) {
		isCanceled = true;
		if(PostHandler.allWaystones.removeByPos(pos)){
			NetworkHandler.netWrap.sendToAll(new BaseUpdateClientMessage().init());
		}
	}

	public void setName(String name) {
		BaseInfo bi = getBaseInfo();
		bi.name = name;
		NetworkHandler.netWrap.sendToServer(new BaseUpdateServerMessage(bi, false));
	}

	public String getName() {
		return getBaseInfo() == null ? "nulll" : getBaseInfo().toString();
	}

	@Override
	public String toString() {
		return getName();
	}
}
