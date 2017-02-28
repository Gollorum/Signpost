package gollorum.signpost.util;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class BlockPos {
	
	public int x, y, z;
	public String world;
	public int dim;

	public BlockPos(World world, int x, int y, int z, int dim){
		this(world.getWorldInfo().getWorldName(), x, y, z, dim);
	}

	public BlockPos(String world, int x, int y, int z, int dim){
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.dim = dim;
	}

	public void writeToNBT(NBTTagCompound tC){
		int[] arr = {x, y, z, dim};
		tC.setIntArray("Position", arr);
		tC.setString("WorldName", world);
	}
	
	public static BlockPos readFromNBT(NBTTagCompound tC){
		int[] arr = tC.getIntArray("Position");
		return new BlockPos(tC.getString("WorldName"), arr[0], arr[1], arr[2], arr[3]);
	}

	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, world);
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
		buf.writeInt(dim);
	}
	
	public static BlockPos fromBytes(ByteBuf buf) {
		String world = ByteBufUtils.readUTF8String(buf);
		int x = buf.readInt();
		int y = buf.readInt();
		int z = buf.readInt();
		int dim = buf.readInt();
		return new BlockPos(world, x, y, z, dim);
	}
	
	@Override
	public boolean equals(Object obj){
		if(!(obj instanceof BlockPos)){
			return false;
		}
		BlockPos other = (BlockPos)obj;
			return other.x==this.x &&
					other.y==this.y &&
					other.z==this.z &&
					(other.dim==this.dim || other.dim==Integer.MIN_VALUE || this.dim == Integer.MIN_VALUE) &&
					(other.world.equals(world) || other.world.equals("") || world.equals(""));
	}
	
	public BlockPos update(BlockPos newPos){
		x = newPos.x;
		y = newPos.y;
		z = newPos.z;
		if(!(newPos.dim==Integer.MIN_VALUE)){
			dim = newPos.dim;
		}
		if(!newPos.world.equals("")){
			world = newPos.world;
		}
		return this;
	}
	
}