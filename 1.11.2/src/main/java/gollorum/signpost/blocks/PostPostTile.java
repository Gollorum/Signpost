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
import gollorum.signpost.util.DoubleBaseInfo.OverlayType;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.math.tracking.DDDVector;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class PostPostTile extends TileEntity {

	public PostType type = PostType.OAK;
	public boolean isItem = false;
	public boolean isCanceled = false;

	@Deprecated
	public DoubleBaseInfo bases = null;

	public PostPostTile(){}

	public PostPostTile(PostType type){
		this();
		this.type = type;
	}
	
	public DoubleBaseInfo getBases(){
		DoubleBaseInfo bases = PostHandler.posts.get(toPos());
		if(bases==null){
			bases = new DoubleBaseInfo(null, null, 0, 0, false, false, null, null, false, false);
			PostHandler.posts.put(toPos(), bases);
		}
		this.bases = bases;
		return bases;
	}

	public void onBlockDestroy(MyBlockPos pos) {
		isCanceled = true;
		DoubleBaseInfo bases = getBases();
		if(bases.overlay1!=null){
			EntityItem item = new EntityItem(world, pos.x, pos.y, pos.z, new ItemStack(bases.overlay1.item, 1));
			world.spawnEntity(item);
		}
		if(bases.overlay2!=null){
			EntityItem item = new EntityItem(world, pos.x, pos.y, pos.z, new ItemStack(bases.overlay2.item, 1));
			world.spawnEntity(item);
		}
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
		tagCompound.setString("overlay1", ""+bases.overlay1);
		tagCompound.setString("overlay2", ""+bases.overlay2);
		tagCompound.setBoolean("point1", bases.point1);
		tagCompound.setBoolean("point2", bases.point2);
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

		final OverlayType overlay1 = OverlayType.get(tagCompound.getString("overlay1"));
		final OverlayType overlay2 = OverlayType.get(tagCompound.getString("overlay2"));

		final boolean point1 = tagCompound.getBoolean("point1");
		final boolean point2 = tagCompound.getBoolean("point2");

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
					bases.overlay1 = overlay1;
					bases.overlay2 = overlay2;
					bases.point1 = point1;
					bases.point2 = point2;
					NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage((PostPostTile) world.getTileEntity(pos), bases));
					return true;
				}
			}
		});
	}

	public static double calcRot1(DoubleBaseInfo tilebases, int x, int z) {
		if(tilebases.point1){
			if(tilebases.base1==null){
				return 0;
			}else{
				int dx = x-tilebases.base1.pos.x;
				int dz = z-tilebases.base1.pos.z;
				return DDDVector.genAngle(dx, dz)+Math.toRadians(-90+(tilebases.flip1?0:180)+(dx<0&&dz>0?180:0));
			}
		}else{
			return Math.toRadians(tilebases.rotation1);
		}
	}

	public static double calcRot2(DoubleBaseInfo tilebases, int x, int z) {
		if(tilebases.point2){
			if(tilebases.base2==null){
				return 0;
			}else{
				int dx = x-tilebases.base2.pos.x;
				int dz = z-tilebases.base2.pos.z;
				return DDDVector.genAngle(dx, dz)+Math.toRadians(-90+(tilebases.flip2?0:180)+(dx<0&&dz>0?180:0));
			}
		}else{
			return Math.toRadians(tilebases.rotation2);
		}
	}

}
