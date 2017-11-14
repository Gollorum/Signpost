package gollorum.signpost.util;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class BaseInfo {
	
	private static final String VERSION = "Version2:";

	public String name;

	public MyBlockPos blockPos;
	/**
	 * One block below the teleport destination
	 */
	public MyBlockPos pos;
	
	public UUID owner;

	public BaseInfo(String name, MyBlockPos pos, UUID owner){
		this.name = ""+name;
		this.blockPos = pos;
		if(pos==null){
			this.pos = null;
		}else{
			this.pos = new MyBlockPos(pos);
		}
		this.owner = owner;
	}

	public BaseInfo(String name, MyBlockPos blockPos, MyBlockPos telePos, UUID owner){
		telePos.y--;
		this.name = ""+name;
		this.blockPos = blockPos;
		this.pos = telePos;
		this.owner = owner;
	}

	public static BaseInfo loadBaseInfo(String name, MyBlockPos blockPos, MyBlockPos telePos, UUID owner){
		telePos.y++;
		return new BaseInfo(""+name, blockPos, telePos, owner);
	}

	public void writeToNBT(NBTTagCompound tC){
		tC.setString("name", ""+name);
		NBTTagCompound posComp = new NBTTagCompound();
		pos.writeToNBT(posComp);
		tC.setTag("pos", posComp);
		NBTTagCompound blockPosComp = new NBTTagCompound();
		pos.writeToNBT(blockPosComp);
		blockPos.writeToNBT(blockPosComp);
		tC.setTag("blockPos", blockPosComp);
		tC.setString("UUID", owner.toString());
	}

	public static BaseInfo readFromNBT(NBTTagCompound tC) {
		String name = tC.getString("name");
		if(tC.hasKey("blockPos")){
			MyBlockPos pos = MyBlockPos.readFromNBT(tC.getCompoundTag("pos"));
			MyBlockPos blockPos = MyBlockPos.readFromNBT(tC.getCompoundTag("blockPos"));
			UUID owner = UUID.fromString(tC.getString("UUID"));
			return loadBaseInfo(name, blockPos, pos, owner);	
		}else{
			MyBlockPos pos = MyBlockPos.readFromNBT(tC);
			UUID owner = UUID.fromString(tC.getString("UUID"));
			return new BaseInfo(name, pos, owner);
		}	
	}

	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, ""+name);
		pos.toBytes(buf);
		ByteBufUtils.writeUTF8String(buf, VERSION+owner.toString());
		blockPos.toBytes(buf);
	}
	
	public static BaseInfo fromBytes(ByteBuf buf) {
		String name = ByteBufUtils.readUTF8String(buf);
		MyBlockPos pos = MyBlockPos.fromBytes(buf);
		String o = ByteBufUtils.readUTF8String(buf);
		if(o.startsWith(VERSION)){
			o = o.replaceFirst(VERSION, "");
			UUID owner = UUID.fromString(o);
			MyBlockPos blockPos = MyBlockPos.fromBytes(buf);
			return loadBaseInfo(name, blockPos, pos, owner);//Ich bin sehr dumm.
		}else{
			UUID owner = UUID.fromString(o);
			return new BaseInfo(name, pos, owner);
		}
	}

	@Override
	public boolean equals(Object other){
		if(!(other instanceof BaseInfo)){
			return super.equals(other);
		}else{
			return ((BaseInfo)other).blockPos.equals(this.blockPos);//Wirklich sehr dumm.
		}
	}
	
	public void setAll(BaseInfo newWS){
		this.name = ""+newWS.name;
		this.pos.update(newWS.pos);
		this.blockPos.update(newWS.blockPos);
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
		return name;
	}
	
	public boolean hasName(){
		return !(name==null || name.equals("null") || name.equals(""));
	}
	
}
