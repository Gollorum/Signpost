package gollorum.signpost.blocks;

import java.util.Map.Entry;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.MyBlockPos;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

public class PostPostTile extends TileEntity {

	public DoubleBaseInfo bases;
	public boolean isItem = true;

	public PostPostTile(){
		bases = new DoubleBaseInfo(null, null, 0, 0, false, false);
		SPEventHandler.scheduleTask(new Runnable(){
			@Override
			public void run() {
				MyBlockPos myPos = toPos();
				if(FMLCommonHandler.instance().getSide().equals(Side.SERVER)){
					PostHandler.posts.put(myPos, bases);
				}else{
					for(Entry<MyBlockPos, DoubleBaseInfo> now: PostHandler.posts.entrySet()){
						if(now.getKey().equals(myPos)){
							bases = now.getValue();
							return;
						}
					}
					PostHandler.posts.put(myPos, bases);
				}
			}
		}, 20);
	}

	public MyBlockPos toPos(){
		if(!worldObj.isRemote){
			return new MyBlockPos(worldObj, pos, dim());
		}else{
			return new MyBlockPos("", pos, dim());
		}
	}
	
	public int dim(){
		if(worldObj==null||worldObj.provider==null){
			return Integer.MIN_VALUE;
		}else
			return worldObj.provider.getDimension();
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
//		if(!FMLCommonHandler.instance().getSide().equals(Side.SERVER)){
//			return tagCompound;
//		}
		tagCompound.setString("base1", ""+bases.base1);
		tagCompound.setString("base2", ""+bases.base2);
		tagCompound.setInteger("rot1", bases.rotation1);
		tagCompound.setInteger("rot2", bases.rotation2);
		tagCompound.setBoolean("flip1", bases.flip1);
		tagCompound.setBoolean("flip2", bases.flip2);
		return tagCompound;
	}

	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
//		System.out.println("read: "+world);
//		if(!FMLCommonHandler.instance().getSide().equals(Side.SERVER)){
//			return;
//		}
		
		bases.base1 = PostHandler.getWSbyName(tagCompound.getString("base1"));
		bases.base2 = PostHandler.getWSbyName(tagCompound.getString("base2"));

		bases.rotation1 = tagCompound.getInteger("rot1");
		bases.rotation2 = tagCompound.getInteger("rot2");

		bases.flip1 = tagCompound.getBoolean("flip1");
		bases.flip2 = tagCompound.getBoolean("flip2");

		if(FMLCommonHandler.instance().getSide().equals(Side.SERVER)){
			SPEventHandler.scheduleTask(new Runnable(){
				@Override
				public void run() {
					NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(toPos(), bases));
				}
			}, 10);
		}
	}
	
}
