package gollorum.signpost.util;

import java.util.UUID;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;

public class BaseInfo {

	public String name;
	public BlockPos pos;
	public UUID owner;

	public BaseInfo(String name, BlockPos pos, UUID owner){
		this.name = name;
		this.pos = pos;
		this.owner = owner;
	}

	public void writeToNBT(NBTTagCompound tC){
		tC.setString("name", name);
		pos.writeToNBT(tC);
		tC.setString("UUID", owner.toString());
	}

	public static BaseInfo readFromNBT(NBTTagCompound tC) {
		String name = tC.getString("name");
		BlockPos pos = BlockPos.readFromNBT(tC);
		UUID owner = UUID.fromString(tC.getString("UUID"));
		return new BaseInfo(name, pos/*, adj*/, owner);		
	}

	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, name);
		pos.toBytes(buf);
		ByteBufUtils.writeUTF8String(buf, owner.toString());
	}
	
	public static BaseInfo fromBytes(ByteBuf buf) {
		String name = ByteBufUtils.readUTF8String(buf);
		BlockPos pos = BlockPos.fromBytes(buf);
		UUID owner = UUID.fromString(ByteBufUtils.readUTF8String(buf));
		return new BaseInfo(name, pos/*, adj*/, owner);
	}

	public boolean sameAs(BaseInfo other){
		return other.pos.equals(this.pos);
	}
	
	public void setAll(BaseInfo newWS){
		this.name = newWS.name;
		this.pos.update(newWS.pos);
		this.owner = newWS.owner;
	}
	
	public boolean update(BaseInfo newWS){
		if(sameAs(newWS)){
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
	
}
