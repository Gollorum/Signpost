package gollorum.signpost.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class MyBlockPos{
	
	public int x, y, z;
	public String world;
	public int dim;
	
	public MyBlockPos(World world, BlockPos pos, int dim){
		this(world, pos.getX(), pos.getY(), pos.getZ(), dim);
	}
	
	public MyBlockPos(World world, int x, int y, int z, int dim){
		this(world.getWorldInfo().getWorldName(), x, y, z, dim);
	}

	public MyBlockPos(String world, BlockPos pos, int dim){
		x = pos.getX();
		y = pos.getY();
		z = pos.getZ();
		this.world = world;
		this.dim = dim;
	}
	public MyBlockPos(String world, int x, int y, int z, int dim){
		this.x = x;
		this.y = y;
		this.z = z;
		this.world = world;
		this.dim = dim;
	}

	public void writeToNBT(NBTTagCompound tC){
		int[] arr = {x, y, z, dim};
		tC.setIntArray("Position", arr);
		tC.setString("WorldName", world);
	}
	
	public static MyBlockPos readFromNBT(NBTTagCompound tC){
		int[] arr = tC.getIntArray("Position");
		return new MyBlockPos(tC.getString("WorldName"), arr[0], arr[1], arr[2], arr[3]);
	}

	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, world);
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
		buf.writeInt(dim);
	}
	
	public static MyBlockPos fromBytes(ByteBuf buf) {
		String world = ByteBufUtils.readUTF8String(buf);
		int x = buf.readInt();
		int y = buf.readInt();
		int z = buf.readInt();
		int dim = buf.readInt();
		return new MyBlockPos(world, x, y, z, dim);
	}
	
	@Override
	public boolean equals(Object obj){
		if(!(obj instanceof MyBlockPos)){
			return super.equals(obj);
		}
		MyBlockPos other = (MyBlockPos)obj;
			return other.x==this.x &&
					other.y==this.y &&
					other.z==this.z &&
					(other.dim==this.dim || other.dim==Integer.MIN_VALUE || this.dim == Integer.MIN_VALUE) &&
					(other.world.equals(world) || other.world.equals("") || world.equals(""));
	}
	
	public MyBlockPos update(MyBlockPos newPos){
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
	
	public BlockPos toBlockPos(){
		return new BlockPos(x, y, z);
	}
	
	public double getLength(){
		return Math.sqrt(x*x+y*y+z*z);
	}
	
	public static double toLength(Vec3i vec){
		return Math.sqrt(vec.getX()*vec.getX()+vec.getY()*vec.getY()+vec.getZ()*vec.getZ());
	}
	
	public static Vec3i normalize(Vec3i vec){
		double length = toLength(vec);
		return new Vec3i(vec.getX()/length, vec.getY()/length, vec.getZ()/length);
	}

	public static double normalizedY(Vec3i vec){
		return vec.getY()/toLength(vec);
	}

	public static double toLength(double x, double y, double z){
		return Math.sqrt(x*x+y*y+z*z);
	}
	
	public static double normalizedY(double x, double y, double z){
		return y/toLength(x, y, z);
	}
	
	@Override
	public String toString(){
		return world+": "+x+"|"+y+"|"+z+" in "+dim;
	}
	
}