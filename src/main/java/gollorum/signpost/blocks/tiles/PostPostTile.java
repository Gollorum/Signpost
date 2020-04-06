package gollorum.signpost.blocks.tiles;

import java.util.LinkedList;
import java.util.List;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.blocks.PostPost;
import gollorum.signpost.blocks.PostPost.Hit;
import gollorum.signpost.blocks.PostPost.HitTarget;
import gollorum.signpost.blocks.PostPost.PostType;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendAllPostBasesMessage;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.BoolRun;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.Paintable;
import gollorum.signpost.util.Sign;
import gollorum.signpost.util.Sign.OverlayType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class PostPostTile extends SuperPostPostTile {

	public PostType type = null;
	private boolean isLoading = false; 

	@Deprecated
	public DoubleBaseInfo bases = null;
	
	public PostPostTile(){
		super();
		SPEventHandler.scheduleTask(new BoolRun(){
			@Override
			public boolean run() {
				if(getBlockType()==null){
					return false;
				}else{
					if(getBlockType() instanceof PostPost){
						type = ((PostPost)getBlockType()).type;
					}
					return true;
				}
			}
		});
	}

	public PostPostTile(PostType type){
		super();
		this.type = type;
	}

	public DoubleBaseInfo getBases(){
		DoubleBaseInfo bases = PostHandler.getPosts().get(toPos());
		if(bases==null){
			if(type ==null){
				bases = new DoubleBaseInfo(PostType.OAK.texture, PostType.OAK.resLocMain);
			}else{
				bases = new DoubleBaseInfo(type.texture, type.resLocMain);
			}
			PostHandler.getPosts().put(toPos(), bases);
		}else{ 
	        if(bases.sign1!=null && bases.sign1.base!=null && bases.sign1.base.teleportPosition ==null){
	          BaseInfo newBase = PostHandler.getWSbyName(bases.sign1.base.getName()); 
	          if(newBase!=null){ 
	            bases.sign1.base = newBase; 
	          } 
	        } 
	        if(bases.sign2!=null && bases.sign2.base!=null && bases.sign2.base.teleportPosition ==null){
	          BaseInfo newBase = PostHandler.getWSbyName(bases.sign2.base.getName()); 
	          if(newBase!=null){ 
	            bases.sign2.base = newBase; 
	          } 
	        } 
		}
		return this.bases = bases;
	}
	
	@Override
	public void onBlockDestroy(MyBlockPos pos) {
		super.onBlockDestroy(pos);
		isCanceled = true;
		DoubleBaseInfo bases = getBases();
		if(bases.sign1.overlay!=null){
			EntityItem item = new EntityItem(getWorld(), pos.x, pos.y, pos.z, new ItemStack(bases.sign1.overlay.item, 1));
			getWorld().spawnEntity(item);
		}
		if(bases.sign2.overlay!=null){
			EntityItem item = new EntityItem(getWorld(), pos.x, pos.y, pos.z, new ItemStack(bases.sign2.overlay.item, 1));
			getWorld().spawnEntity(item);
		}
		if(PostHandler.getPosts().remove(pos)!=null){
			NetworkHandler.netWrap.sendToAll(new SendAllPostBasesMessage());
		}
	}

	@Override
	public void save(NBTTagCompound tagCompound) {
		DoubleBaseInfo bases = getBases();
		tagCompound.setString("base1", ""+bases.sign1.base);
		tagCompound.setString("base2", ""+bases.sign2.base);
		tagCompound.setInteger("rot1", bases.sign1.rotation);
		tagCompound.setInteger("rot2", bases.sign2.rotation);
		tagCompound.setBoolean("flip1", bases.sign1.flip);
		tagCompound.setBoolean("flip2", bases.sign2.flip);
		tagCompound.setString("overlay1", ""+bases.sign1.overlay);
		tagCompound.setString("overlay2", ""+bases.sign2.overlay);
		tagCompound.setBoolean("point1", bases.sign1.point);
		tagCompound.setBoolean("point2", bases.sign2.point);
		tagCompound.setString("paint1", SuperPostPostTile.locToString(bases.sign1.paint));
		tagCompound.setString("paint2", SuperPostPostTile.locToString(bases.sign2.paint));
		tagCompound.setString("postPaint", SuperPostPostTile.locToString(bases.postPaint));
		
		if(bases.equals(bases.paintObject)){
			tagCompound.setByte("paintObjectIndex", (byte)1);
		}else if(bases.sign1.equals(bases.paintObject)){
			tagCompound.setByte("paintObjectIndex", (byte)2);
		}else if(bases.sign2.equals(bases.paintObject)){
			tagCompound.setByte("paintObjectIndex", (byte)3);
		}else{
			tagCompound.setByte("paintObjectIndex", (byte)0);
		}
	}

	@Override
	public void load(NBTTagCompound tagCompound) {
	    isLoading = true;
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

		final String paint1 = tagCompound.getString("paint1");
		final String paint2 = tagCompound.getString("paint2");
		
		final String postPaint = tagCompound.getString("postPaint");
		
		final PostPostTile self = this;

		final byte paintObjectIndex = tagCompound.getByte("paintObjectIndex");

		SPEventHandler.scheduleTask(new BoolRun(){
			@Override
			public boolean run() {
				if(getWorld()==null || type==null){
					return false;
				}else{
					if(getWorld().isRemote){
						return true;
					}
					DoubleBaseInfo bases = getBases();
					bases.sign1.base = PostHandler.getForceWSbyName(base1);
					bases.sign2.base = PostHandler.getForceWSbyName(base2);
					bases.sign1.rotation = rotation1;
					bases.sign2.rotation = rotation2;
					bases.sign1.flip = flip1;
					bases.sign2.flip = flip2;
					bases.sign1.overlay = overlay1;
					bases.sign2.overlay = overlay2;
					bases.sign1.point = point1;
					bases.sign2.point = point2;
					bases.sign1.paint = stringToLoc(paint1);
					bases.sign2.paint = stringToLoc(paint2);
					bases.postPaint = postPaint==null || postPaint.equals("") || postPaint.equals("null") || postPaint.equals("minecraft:") ? type.resLocMain : stringToLoc(postPaint);
					switch(paintObjectIndex){
					case 1:
						bases.paintObject = bases;
						bases.awaitingPaint = true;
						break;
					case 2:
						bases.paintObject = bases.sign1;
						bases.awaitingPaint = true;
						break;
					case 3:
						bases.paintObject = bases.sign2;
						bases.awaitingPaint = true;
						break;
					default:
						bases.paintObject = null;
						bases.awaitingPaint = false;
						break;
					}
					NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(self, bases));
					isLoading = false;
					return true;
				}
			}
		});
	}
    
	public boolean isLoading() {
		return isLoading;
	}

	@Override
	public Sign getSign(EntityPlayer player) {
		DoubleBaseInfo bases = getBases();
		Hit hit = (Hit) ((PostPost)getBlockType()).getHitTarget(getWorld(), pos.getX(), pos.getY(), pos.getZ(), player);
		if(hit.target.equals(HitTarget.BASE1)){
			return bases.sign1;
		}else if(hit.target.equals(HitTarget.BASE2)){
			return bases.sign2;
		}else{
			return null;
		}
	}

	@Override
	public Paintable getPaintable(EntityPlayer player) {
		DoubleBaseInfo bases = getBases();
		Hit hit = (Hit) ((PostPost)getBlockType()).getHitTarget(getWorld(), pos.getX(), pos.getY(), pos.getZ(), player);
		if(hit.target.equals(HitTarget.BASE1)){
			return bases.sign1;
		}else if(hit.target.equals(HitTarget.BASE2)){
			return bases.sign2;
		}else{
			return bases;
		}
	}
	
	@Override
	public ResourceLocation getPostPaint(){
		return getBases().postPaint;
	}
	
	public void setPostPaint(ResourceLocation loc){
		getBases().postPaint = loc;
	}

	@Override
	public boolean isAwaitingPaint() {
		return getBases().awaitingPaint;
	}
	

	@Override
	public Paintable getPaintObject() {
		return getBases().paintObject;
	}
	
	@Override
	public void setAwaitingPaint(boolean awaitingPaint){
		getBases().awaitingPaint = awaitingPaint;
	}
	
	@Override
	public void setPaintObject(Paintable paintObject){
		getBases().paintObject = paintObject;
	}

	@Override
	public String toString(){
		return getBases()+" at "+toPos();
	}

	@Override
	public List<Sign> getEmptySigns() {
		List<Sign> ret = new LinkedList<Sign>();
		DoubleBaseInfo bases = getBases();
		if (bases.sign1 == null || !bases.sign1.isValid()) {
			ret.add(bases.sign1);
		}
		if (bases.sign2 == null || !bases.sign2.isValid()) {
			ret.add(bases.sign2);
		}
		return ret;
	}
}
