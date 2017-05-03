package gollorum.signpost.blocks;

import java.util.UUID;

import gollorum.signpost.util.BlockPos;
import gollorum.signpost.util.Sign;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public abstract class SuperPostPostTile extends TileEntity{
	
	public boolean isItem = false;
	public boolean isCanceled = false;
	public UUID owner;

	public final BlockPos toPos(){
		if(worldObj==null||worldObj.isRemote){
			return new BlockPos("", xCoord, yCoord, zCoord, dim());
		}else{
			return new BlockPos(worldObj.getWorldInfo().getWorldName(), xCoord, yCoord, zCoord, dim());
		}
	}

	public final int dim(){
		if(worldObj==null||worldObj.provider==null){
			return Integer.MIN_VALUE;
		}else
			return worldObj.provider.dimensionId;
	}

	public static final ResourceLocation stringToLoc(String str){
		return str==null||str.equals("null")||str.equals("")?null:new ResourceLocation(str);
	}
	
	public static final String LocToString(ResourceLocation loc){
		return loc==null?"null":loc.getResourcePath();
	}

	public abstract void onBlockDestroy(BlockPos pos);
	
	@Override
	public void writeToNBT(NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		tagCompound.setInteger("signpostNBTVersion", 1);
		NBTTagCompound tagComp = new NBTTagCompound();
		if(!(owner == null)){
			tagComp.setString("PostOwner", owner.toString());
		}
		save(tagComp);
		tagCompound.setTag("signpostDataTag", tagComp);
	}

	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		if(tagCompound.getInteger("signpostNBTVersion")==1){
			NBTTagCompound tagComp = (NBTTagCompound) tagCompound.getTag("signpostDataTag");
			owner = UUID.fromString(tagComp.getString("PostOwner"));
			load(tagComp);
		}else{
			load(tagCompound);
		}
	}

	public abstract void save(NBTTagCompound tagCompound);
	public abstract void load(NBTTagCompound tagCompound);
	
	public abstract Sign getSign(EntityPlayer player);
}
