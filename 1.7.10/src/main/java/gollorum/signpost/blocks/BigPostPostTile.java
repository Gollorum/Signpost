package gollorum.signpost.blocks;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.blocks.BigPostPost.BigPostType;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendAllBigPostBasesMessage;
import gollorum.signpost.network.messages.SendBigPostBasesMessage;
import gollorum.signpost.util.BigBaseInfo;
import gollorum.signpost.util.BigBaseInfo.OverlayType;
import gollorum.signpost.util.BlockPos;
import gollorum.signpost.util.BoolRun;
import gollorum.signpost.util.math.tracking.DDDVector;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class BigPostPostTile extends TileEntity {

	public BigPostType type = BigPostType.OAK;
	public boolean isItem = false;
	public boolean isCanceled = false;
	public static final int DESCRIPTIONLENGTH = 4;

	@Deprecated
	public BigBaseInfo bases = null;

	public BigPostPostTile(){}

	public BigPostPostTile(BigPostType type){
		this();
		this.type = type;
	}
	
	public BigBaseInfo getBases(){
		BigBaseInfo bases = PostHandler.bigPosts.get(toPos());
		if(bases==null){
			bases = new BigBaseInfo(null, 0, false, null, false);
			PostHandler.bigPosts.put(toPos(), bases);
		}
		this.bases = bases;
		return bases;
	}
	
	public void onBlockDestroy(BlockPos pos) {
		isCanceled = true;
		BigBaseInfo bases = getBases();
		if(bases.overlay!=null){
			EntityItem item = new EntityItem(worldObj, pos.x, pos.y, pos.z, new ItemStack(bases.overlay.item, 1));
			worldObj.spawnEntityInWorld(item);
		}
		if(PostHandler.bigPosts.remove(pos)!=null){
			NetworkHandler.netWrap.sendToAll(new SendAllBigPostBasesMessage());
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
		BigBaseInfo bases = getBases();
		tagCompound.setString("base", ""+bases.base);
		tagCompound.setInteger("rot", bases.rotation);
		tagCompound.setBoolean("flip", bases.flip);
		tagCompound.setString("overlay", ""+bases.overlay);
		tagCompound.setBoolean("point", bases.point);
		for(int i=0; i<bases.description.length; i++){
			tagCompound.setString("description"+i, bases.description[i]);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		final String base = tagCompound.getString("base");
		final int rotation = tagCompound.getInteger("rot");
		final boolean flip = tagCompound.getBoolean("flip");
		final OverlayType overlay = OverlayType.get(tagCompound.getString("overlay"));
		final boolean point = tagCompound.getBoolean("point");
		final String[] description = new String[DESCRIPTIONLENGTH];

		for(int i=0; i<DESCRIPTIONLENGTH; i++){
			description[i] = tagCompound.getString("description"+i);
		}
		
		SPEventHandler.scheduleTask(new BoolRun(){
			@Override
			public boolean run() {
				if(worldObj==null){
					return false;
				}else{
					if(worldObj.isRemote){
						return true;
					}
					BigBaseInfo bases = getBases();
					bases.base = PostHandler.getWSbyName(base);
					bases.rotation = rotation;
					bases.flip = flip;
					bases.overlay = overlay;
					bases.point = point;
					bases.description = description;
					NetworkHandler.netWrap.sendToAll(new SendBigPostBasesMessage((BigPostPostTile) worldObj.getTileEntity(xCoord, yCoord, zCoord), bases));
					return true;
				}
			}
		});
	}

	public static double calcRot(BigBaseInfo tilebases, int x, int z) {
 		if(tilebases.point&&!(tilebases.base==null||tilebases.base.pos==null||ConfigHandler.deactivateTeleportation)){
			int dx = x-tilebases.base.pos.x;
			int dz = z-tilebases.base.pos.z;
			return DDDVector.genAngle(dx, dz)+Math.toRadians(-90+(tilebases.flip?0:180)+(dx<0&&dz>0?180:0));
		}else{
			return Math.toRadians(tilebases.rotation);
		}
	}
}
