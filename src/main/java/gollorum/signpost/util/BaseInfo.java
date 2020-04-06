package gollorum.signpost.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.Objects;
import java.util.UUID;

public class BaseInfo {

	private static final String VERSION = "Version2:";
	private String name;
	public MyBlockPos blockPosition;
	/**
	 * One block below the teleport destination
	 */
	public MyBlockPos teleportPosition;
	/** unused */
	public UUID owner;

	public BaseInfo(String name, MyBlockPos teleportPosition, UUID owner){
		this.name = ""+name;
		this.blockPosition = teleportPosition;
		if(teleportPosition ==null){
			this.teleportPosition = null;
		}else{
			this.teleportPosition = new MyBlockPos(teleportPosition);
		}
		this.owner = owner;
	}
	
	public BaseInfo(String name, MyBlockPos blockPosition, MyBlockPos telePos, UUID owner){
		telePos.y--;
		this.name = ""+name;
		this.blockPosition = blockPosition;
		this.teleportPosition = telePos;
		this.owner = owner;
	}

	public static BaseInfo loadBaseInfo(String name, MyBlockPos blockPos, MyBlockPos telePos, UUID owner){
		telePos.y++;
		return new BaseInfo(name, blockPos, telePos, owner);
	}

	public void writeToNBT(NBTTagCompound tC){
		tC.setString("name", ""+name);	//Warum bin ich nur so unglaublich geh�rnamputiert? *kotz*
		NBTTagCompound posComp = new NBTTagCompound();
		teleportPosition.writeToNBT(posComp);
		tC.setTag("pos", posComp);
		NBTTagCompound blockPosComp = new NBTTagCompound();
		teleportPosition.writeToNBT(blockPosComp);
		blockPosition.writeToNBT(blockPosComp);
		tC.setTag("blockPos", blockPosComp);
		teleportPosition.writeToNBT(tC);
		tC.setString("UUID", ""+owner);
	}

	public static BaseInfo readFromNBT(NBTTagCompound tC) {
		String name = tC.getString("name");
		UUID owner = uuidFromString(tC.getString("UUID"));
		if(tC.hasKey("blockPos")){
			MyBlockPos pos = MyBlockPos.readFromNBT(tC.getCompoundTag("pos"));
			MyBlockPos blockPos = MyBlockPos.readFromNBT(tC.getCompoundTag("blockPos"));
			return loadBaseInfo(name, blockPos, pos, owner);
		}else{
			MyBlockPos pos = MyBlockPos.readFromNBT(tC);
			return new BaseInfo(name, pos, owner);
		}
	}

	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, ""+name);
		teleportPosition.toBytes(buf);
		ByteBufUtils.writeUTF8String(buf, VERSION+owner);
		blockPosition.toBytes(buf);
	}
	
	public static BaseInfo fromBytes(ByteBuf buf) {
		String name = ByteBufUtils.readUTF8String(buf);
		MyBlockPos pos = MyBlockPos.fromBytes(buf);
		String o = ByteBufUtils.readUTF8String(buf);
		if(o.startsWith(VERSION)){
			o = o.replaceFirst(VERSION, "");
			UUID owner;
			try{
				owner = uuidFromString(o);
			}catch(Exception e){
				owner = null;
			}
			MyBlockPos blockPos = MyBlockPos.fromBytes(buf);
			return loadBaseInfo(name, blockPos, pos, owner);//Ich bin sehr dumm.
		}else{
			UUID owner = uuidFromString(o);
			return new BaseInfo(name, pos, owner);
		}
	}

	@Override
	public boolean equals(Object other){
		if(!(other instanceof BaseInfo)){
			return super.equals(other);
		} else {
			return ((BaseInfo)other).blockPosition.equals(this.blockPosition);//Wirklich sehr dumm.
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(blockPosition);
	}



	public void setAll(BaseInfo newWS){
		this.name = ""+newWS.name;
		this.teleportPosition.update(newWS.teleportPosition);
		this.blockPosition.update(newWS.blockPosition);
		this.owner = newWS.owner;
	}
	
	public boolean update(BaseInfo newWS){
		if(equals(newWS)){
			setAll(newWS);
			return true;
		}else{
			return false;
		}
	}

	@Override
	public String toString(){
		return ""+name;
	}
	
	public boolean hasName(){
		return !(name==null || name.equals("null") || name.equals(""));
	}
	
	public String getName(){
		return toString();
	}
	
	public void setName(String name){
		this.name = name;
	}



	public static BaseInfo fromExternal(String name, int blockX, int blockY, int blockZ, int teleX, int teleY, int teleZ, int dimension, String modId){
		MyBlockPos blockPos = new MyBlockPos(blockX, blockY, blockZ, dimension);
		MyBlockPos telePos = new MyBlockPos(teleX, teleY, teleZ, dimension);
		return new BaseInfo(name, blockPos, telePos, null);
	}
	
	private static UUID uuidFromString(String string){
		try{
			return UUID.fromString(string);
		}catch(IllegalArgumentException e){
			return null;
		}
	}
}
