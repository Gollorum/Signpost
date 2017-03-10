package gollorum.signpost.blocks;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.blocks.PostPost.PostType;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendAllPostBasesMessage;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.BoolRun;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.MyBlockPos;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class PostPostTile extends TileEntity {

	public PostType type = PostType.OAK;
	public boolean isItem = false;
	public boolean isCanceled = false;

	public PostPostTile(){}


	public PostPostTile(PostType type){
		this();
		this.type = type;
	}
	
	public DoubleBaseInfo getBases(){
		DoubleBaseInfo bases = PostHandler.posts.get(toPos());
		if(bases==null){
			bases = new DoubleBaseInfo(null, null, 0, 0, false, false);
			PostHandler.posts.put(toPos(), bases);
		}
		return bases;
	}

	public void onBlockDestroy(MyBlockPos pos) {
		isCanceled = true;
		if(PostHandler.posts.remove(pos)!=null){
			NetworkHandler.netWrap.sendToAll(new SendAllPostBasesMessage());
		}
	}

	public MyBlockPos toPos(){
		if(!world.isRemote){
			return new MyBlockPos(world, pos, dim());
		}else{
			return new MyBlockPos("", pos, dim());
		}
	}
	
	public int dim(){
		if(world==null||world.provider==null){
			return Integer.MIN_VALUE;
		}else
			return world.provider.getDimension();
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		DoubleBaseInfo bases = getBases();
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

		final BaseInfo base1 = PostHandler.getWSbyName(tagCompound.getString("base1"));
		final BaseInfo base2 = PostHandler.getWSbyName(tagCompound.getString("base2"));

		final int rotation1 = tagCompound.getInteger("rot1");
		final int rotation2 = tagCompound.getInteger("rot2");

		final boolean flip1 = tagCompound.getBoolean("flip1");
		final boolean flip2 = tagCompound.getBoolean("flip2");

		SPEventHandler.scheduleTask(new BoolRun(){
			@Override
			public boolean run() {
				if(world==null){
					return false;
				}else{
					if(world.isRemote){
						return true;
					}
					DoubleBaseInfo bases = getBases();
					bases.base1 = base1;
					bases.base2 = base2;
					bases.rotation1 = rotation1;
					bases.rotation2 = rotation2;
					bases.flip1 = flip1;
					bases.flip2 = flip2;
					NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(toPos(), bases));
					return true;
				}
			}
		});
	}

}
