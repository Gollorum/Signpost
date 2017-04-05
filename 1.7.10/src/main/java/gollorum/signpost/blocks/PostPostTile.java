package gollorum.signpost.blocks;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.blocks.PostPost.PostType;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendAllPostBasesMessage;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.util.BlockPos;
import gollorum.signpost.util.BoolRun;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.DoubleBaseInfo.OverlayType;
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
	
	public void onBlockDestroy(BlockPos pos) {
		isCanceled = true;
		DoubleBaseInfo bases = getBases();
		if(bases.overlay1!=null){
			EntityItem item = new EntityItem(worldObj, pos.x, pos.y, pos.z, new ItemStack(bases.overlay1.item, 1));
			worldObj.spawnEntityInWorld(item);
		}
		if(bases.overlay2!=null){
			EntityItem item = new EntityItem(worldObj, pos.x, pos.y, pos.z, new ItemStack(bases.overlay2.item, 1));
			worldObj.spawnEntityInWorld(item);
		}
		if(PostHandler.posts.remove(pos)!=null){
			NetworkHandler.netWrap.sendToAll(new SendAllPostBasesMessage());
		}
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
	
	@Override
	public void writeToNBT(NBTTagCompound tagCompound) {
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
	}

	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		final String base1 = tagCompound.getString("base1");
		final String base2 = tagCompound.getString("base2");

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
				if(worldObj==null){
					return false;
				}else{
					if(worldObj.isRemote){
						return true;
					}
					DoubleBaseInfo bases = getBases();
					bases.base1 = PostHandler.getWSbyName(base1);
					bases.base2 = PostHandler.getWSbyName(base2);
					bases.rotation1 = rotation1;
					bases.rotation2 = rotation2;
					bases.flip1 = flip1;
					bases.flip2 = flip2;
					bases.overlay1 = overlay1;
					bases.overlay2 = overlay2;
					bases.point1 = point1;
					bases.point2 = point2;
					NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage((PostPostTile) worldObj.getTileEntity(xCoord, yCoord, zCoord), bases));
					return true;
				}
			}
		});
	}

	public static double calcRot1(DoubleBaseInfo tilebases, int x, int z) {
 		if(tilebases.point1&&!(tilebases.base1==null||tilebases.base1.pos==null||ConfigHandler.deactivateTeleportation)){
			int dx = x-tilebases.base1.pos.x;
			int dz = z-tilebases.base1.pos.z;
			return DDDVector.genAngle(dx, dz)+Math.toRadians(-90+(tilebases.flip1?0:180)+(dx<0&&dz>0?180:0));
		}else{
			return Math.toRadians(tilebases.rotation1);
		}
	}

	public static double calcRot2(DoubleBaseInfo tilebases, int x, int z) {
		if(tilebases.point2&&!(tilebases.base2==null||tilebases.base2.pos==null||ConfigHandler.deactivateTeleportation)){
			int dx = x-tilebases.base2.pos.x;
			int dz = z-tilebases.base2.pos.z;
			return DDDVector.genAngle(dx, dz)+Math.toRadians(-90+(tilebases.flip2?0:180)+(dx<0&&dz>0?180:0));
		}else{
			return Math.toRadians(tilebases.rotation2);
		}
	}

}
