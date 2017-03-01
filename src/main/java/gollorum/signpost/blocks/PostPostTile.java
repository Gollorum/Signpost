package gollorum.signpost.blocks;

import java.util.Map.Entry;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.Signpost;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendAllPostBasesMessage;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.util.BlockPos;
import gollorum.signpost.util.DoubleBaseInfo;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class PostPostTile extends TileEntity {

	public DoubleBaseInfo bases;
	public boolean isItem = true;

	public PostPostTile(){
		bases = new DoubleBaseInfo(null, null, 0, 0, false, false);
		SPEventHandler.scheduleTask(new Runnable(){
			@Override
			public void run() {
				BlockPos myPos = toPos();
				if(Signpost.serverSide){
					PostHandler.posts.put(myPos, bases);
				}else{
					for(Entry<BlockPos, DoubleBaseInfo> now: PostHandler.posts.entrySet()){
						if(now.getKey().equals(myPos)){
							bases = now.getValue();
							return;
						}
					}
					PostHandler.posts.put(myPos, bases);
				}
			}
		}, 10);
	}

	public void onBlockDestroy() {
		if(PostHandler.posts.remove(toPos())!=null){
			NetworkHandler.netWrap.sendToAll(new SendAllPostBasesMessage());
		}
	}

	public BlockPos toPos(){
		if(Signpost.serverSide){
			return new BlockPos(worldObj, xCoord, yCoord, zCoord, dim());
		}else{
			return new BlockPos("", xCoord, yCoord, zCoord, dim());
		}
	}
	
	public int dim(){
		if(worldObj==null||worldObj.provider==null){
			return Integer.MIN_VALUE;
		}else
			return worldObj.provider.dimensionId;
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		tagCompound.setString("base1", ""+bases.base1);
		tagCompound.setString("base2", ""+bases.base2);
		tagCompound.setInteger("rot1", bases.rotation1);
		tagCompound.setInteger("rot2", bases.rotation2);
		tagCompound.setBoolean("flip1", bases.flip1);
		tagCompound.setBoolean("flip2", bases.flip2);
	}

	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		bases.base1 = PostHandler.getWSbyName(tagCompound.getString("base1"));
		bases.base2 = PostHandler.getWSbyName(tagCompound.getString("base2"));

		bases.rotation1 = tagCompound.getInteger("rot1");
		bases.rotation2 = tagCompound.getInteger("rot2");

		bases.flip1 = tagCompound.getBoolean("flip1");
		bases.flip2 = tagCompound.getBoolean("flip2");
		
		SPEventHandler.scheduleTask(new Runnable(){
			@Override
			public void run() {
				NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(new BlockPos(worldObj, xCoord, yCoord, zCoord, dim()), bases));
			}
		}, 10);
	}
	
}
