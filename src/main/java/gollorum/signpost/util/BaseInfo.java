package gollorum.signpost.util;

import java.util.UUID;

import gollorum.signpost.Signpost;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class BaseInfo {

	private static final String VERSION = "Version2:";
	private String name;
	public MyBlockPos blockPos;
	/**
	 * One block below the teleport destination
	 */
	public MyBlockPos pos;
	/** unused */
	public UUID owner;

	public BaseInfo(String name, MyBlockPos pos, UUID owner) {
		this.setName("" + name);
		this.blockPos = pos;
		if (pos == null) {
			this.pos = null;
		} else {
			this.pos = new MyBlockPos(pos);
		}
		this.owner = owner;
	}

	public BaseInfo(String name, MyBlockPos blockPos, MyBlockPos telePos, UUID owner) {
		telePos.y--;
		this.setName("" + name);
		this.blockPos = blockPos;
		this.pos = telePos;
		this.owner = owner;
	}

	public static BaseInfo loadBaseInfo(String name, MyBlockPos blockPos, MyBlockPos telePos, UUID owner) {
		telePos.y++;
		return new BaseInfo(name, blockPos, telePos, owner);
	}

	public void writeToNBT(NBTTagCompound tC) {
		tC.setString("name", "" + getName()); // Warum bin ich nur so unglaublich
											// gehörnamputiert? *kotz*
		NBTTagCompound posComp = new NBTTagCompound();
		pos.writeToNBT(posComp);
		tC.setTag("pos", posComp);
		NBTTagCompound blockPosComp = new NBTTagCompound();
		pos.writeToNBT(blockPosComp);
		blockPos.writeToNBT(blockPosComp);
		tC.setTag("blockPos", blockPosComp);
		pos.writeToNBT(tC);
		tC.setString("UUID", "" + owner);
	}

	public static BaseInfo readFromNBT(NBTTagCompound tC) {
		String name = tC.getString("name");
		if (tC.hasKey("blockPos")) {
			MyBlockPos pos = MyBlockPos.readFromNBT(tC.getCompoundTag("pos"));
			MyBlockPos blockPos = MyBlockPos.readFromNBT(tC.getCompoundTag("blockPos"));
			UUID owner = uuidFromString(tC.getString("UUID"));
			return loadBaseInfo(name, blockPos, pos, owner);
		} else {
			MyBlockPos pos = MyBlockPos.readFromNBT(tC);
			UUID owner = uuidFromString(tC.getString("UUID"));
			return new BaseInfo(name, pos, owner);
		}
	}

	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, "" + getName());
		pos.toBytes(buf);
		ByteBufUtils.writeUTF8String(buf, VERSION + owner);
		blockPos.toBytes(buf);
	}

	public static BaseInfo fromBytes(ByteBuf buf) {
		String name = ByteBufUtils.readUTF8String(buf);
		MyBlockPos pos = MyBlockPos.fromBytes(buf);
		String o = ByteBufUtils.readUTF8String(buf);
		if (o.startsWith(VERSION)) {
			o = o.replaceFirst(VERSION, "");
			UUID owner;
			try {
				owner = uuidFromString(o);
			} catch (Exception e) {
				owner = null;
			}
			MyBlockPos blockPos = MyBlockPos.fromBytes(buf);
			return loadBaseInfo(name, blockPos, pos, owner);// Ich bin sehr
															// dumm.
		} else {
			UUID owner = uuidFromString(o);
			return new BaseInfo(name, pos, owner);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof BaseInfo)) {
			return super.equals(other);
		} else {
			return ((BaseInfo) other).blockPos.equals(this.blockPos);// Wirklich
																		// sehr
																		// dumm.
		}
	}

	public void setAll(BaseInfo newWS) {
		this.setName("" + newWS.getName());
		this.pos.update(newWS.pos);
		this.blockPos.update(newWS.blockPos);
		this.owner = newWS.owner;
	}

	public boolean update(BaseInfo newWS) {
		if (equals(newWS)) {
			setAll(newWS);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String toString(){
		return ""+getName();
	}
	
	public boolean hasName(){
		return !(getName()==null || getName().equals("null") || getName().equals(""));
	}
	
	public String getName(){
		return toString();
	}

	public void setName(String name){
		this.name = name;
	}
	
	public boolean isNative() {
		return blockPos.modID.equals(Signpost.MODID);
	}

	public static BaseInfo fromExternal(String name, int x, int y, int z, int dimension, String modId) {
		World world = FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(dimension);
		String worldString;
		try {
			worldString = world.getWorldInfo().getWorldName();
		} catch (Exception e) {
			worldString = "";
		}
		MyBlockPos pos = new MyBlockPos(worldString, x, y, z, dimension, modId);
		return new BaseInfo(name, pos, null);
	}

	public static BaseInfo fromExternal(String name, int blockX, int blockY, int blockZ, int teleX, int teleY,
			int teleZ, int dimension, String modId) {
		String worldString;
		try {
			World world = FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(dimension);
			worldString = world.getWorldInfo().getWorldName();
		} catch (Exception e) {
			worldString = "";
		}
		MyBlockPos blockPos = new MyBlockPos(worldString, blockX, blockY, blockZ, dimension, modId);
		MyBlockPos telePos = new MyBlockPos(worldString, teleX, teleY, teleZ, dimension, modId);
		return new BaseInfo(name, blockPos, telePos, null);
	}

	private static UUID uuidFromString(String string) {
		try {
			return UUID.fromString(string);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
