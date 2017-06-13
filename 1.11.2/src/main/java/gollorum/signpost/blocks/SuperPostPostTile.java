package gollorum.signpost.blocks;

import java.util.UUID;

import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.Sign;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public abstract class SuperPostPostTile extends TileEntity {

	public boolean isItem = false;
	public boolean isCanceled = false;
	public UUID owner;
	
	public final MyBlockPos toPos(){
		if(world==null||world.isRemote){
			return new MyBlockPos("", pos.getX(), pos.getY(), pos.getZ(), dim());
		}else{
			return new MyBlockPos(world.getWorldInfo().getWorldName(), pos.getX(), pos.getY(), pos.getZ(), dim());
		}
	}

	public final int dim(){
		if(world==null||world.provider==null){
			return Integer.MIN_VALUE;
		}else
			return world.provider.getDimension();
	}

	public static final ResourceLocation stringToLoc(String str){
		return str==null||str.equals("null")||str.equals("")?null:new ResourceLocation(str);
	}
	
	public static final String locToString(ResourceLocation loc){
		return loc==null?"null":loc.getResourceDomain()+":"+loc.getResourcePath();
	}

	public abstract void onBlockDestroy(MyBlockPos pos);
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		tagCompound.setInteger("signpostNBTVersion", 1);
		NBTTagCompound tagComp = new NBTTagCompound();
		if(!(owner == null)){
			tagComp.setString("PostOwner", owner.toString());
		}
		save(tagComp);
		tagCompound.setTag("signpostDataTag", tagComp);
		return tagCompound;
	}

	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		if(tagCompound.getInteger("signpostNBTVersion")==1){
			NBTTagCompound tagComp = (NBTTagCompound) tagCompound.getTag("signpostDataTag");
			String owner = tagComp.getString("PostOwner");
			try{
				this.owner = owner==null ?  null : UUID.fromString(owner);
			}catch(Exception e){
				this.owner = null;
			}
			load(tagComp);
		}else{
			load(tagCompound);
		}
	}

	public abstract void save(NBTTagCompound tagCompound);
	public abstract void load(NBTTagCompound tagCompound);
	
	public abstract Sign getSign(EntityPlayer player);
}
