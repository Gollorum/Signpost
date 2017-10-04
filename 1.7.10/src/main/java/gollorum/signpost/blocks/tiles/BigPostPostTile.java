package gollorum.signpost.blocks.tiles;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.blocks.BigPostPost;
import gollorum.signpost.blocks.BigPostPost.BigHit;
import gollorum.signpost.blocks.BigPostPost.BigHitTarget;
import gollorum.signpost.blocks.BigPostPost.BigPostType;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendAllBigPostBasesMessage;
import gollorum.signpost.network.messages.SendBigPostBasesMessage;
import gollorum.signpost.util.BigBaseInfo;
import gollorum.signpost.util.BoolRun;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.Sign;
import gollorum.signpost.util.Sign.OverlayType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class BigPostPostTile extends SuperPostPostTile {

	public BigPostType type = BigPostType.OAK;
	public static final int DESCRIPTIONLENGTH = 4;

	@Deprecated
	public BigBaseInfo bases = null;

	public BigPostPostTile(){super();}

	public BigPostPostTile(BigPostType type){
		this();
		this.type = type;
	}
	
	public BigBaseInfo getBases(){
		BigBaseInfo bases = PostHandler.getBigPosts().get(toPos());
		if(bases==null){
			bases = new BigBaseInfo(type.texture, type.resLocMain);
			PostHandler.getBigPosts().put(toPos(), bases);
		}
		this.bases = bases;
		return bases;
	}

	@Override
	public void onBlockDestroy(MyBlockPos pos) {
		super.onBlockDestroy(pos);
		isCanceled = true;
		BigBaseInfo bases = getBases();
		if(bases.sign.overlay!=null){
			EntityItem item = new EntityItem(worldObj, pos.x, pos.y, pos.z, new ItemStack(bases.sign.overlay.item, 1));
			worldObj.spawnEntityInWorld(item);
		}
		if(PostHandler.getBigPosts().remove(pos)!=null){
			NetworkHandler.netWrap.sendToAll(new SendAllBigPostBasesMessage());
		}
	}

	@Override
	public void save(NBTTagCompound tagCompound) {
		BigBaseInfo bases = getBases();
		tagCompound.setString("base", ""+bases.sign.base);
		tagCompound.setInteger("rot", bases.sign.rotation);
		tagCompound.setBoolean("flip", bases.sign.flip);
		tagCompound.setString("overlay", ""+bases.sign.overlay);
		tagCompound.setBoolean("point", bases.sign.point);
		tagCompound.setString("paint", locToString(bases.sign.paint));
		tagCompound.setString("postPaint", locToString(bases.postPaint));
		for(int i=0; i<bases.description.length; i++){
			tagCompound.setString("description"+i, bases.description[i]);
		}
	}

	@Override
	public void load(NBTTagCompound tagCompound) {
		final String base = tagCompound.getString("base");
		final int rotation = tagCompound.getInteger("rot");
		final boolean flip = tagCompound.getBoolean("flip");
		final OverlayType overlay = OverlayType.get(tagCompound.getString("overlay"));
		final boolean point = tagCompound.getBoolean("point");
		final String[] description = new String[DESCRIPTIONLENGTH];
		final String paint = tagCompound.getString("paint");
		final String postPaint = tagCompound.getString("postPaint");

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
					bases.sign.base = PostHandler.getForceWSbyName(base);
					bases.sign.rotation = rotation;
					bases.sign.flip = flip;
					bases.sign.overlay = overlay;
					bases.sign.point = point;
					bases.description = description;
					bases.sign.paint = stringToLoc(paint);
					bases.postPaint = postPaint==null || postPaint.equals("") || postPaint.equals("null") || postPaint.equals("minecraft:") ? type.resLocMain : stringToLoc(postPaint);
					NetworkHandler.netWrap.sendToAll(new SendBigPostBasesMessage((BigPostPostTile) worldObj.getTileEntity(xCoord, yCoord, zCoord), bases));
					return true;
				}
			}
		});
	}

	@Override
	public Sign getSign(EntityPlayer player) {
		BigBaseInfo bases = getBases();
		BigHit hit = (BigHit) ((BigPostPost)blockType).getHitTarget(worldObj, xCoord, yCoord, zCoord, player);
		if(hit.target.equals(BigHitTarget.BASE)){
			return bases.sign;
		}else{
			return null;
		}
	}

	@Override
	public ResourceLocation getPostPaint(){
		return getBases().postPaint;
	}

	public void setPostPaint(ResourceLocation loc){
		getBases().postPaint = loc;
	}
}
