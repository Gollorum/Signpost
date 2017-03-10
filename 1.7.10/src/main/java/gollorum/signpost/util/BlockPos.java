package gollorum.signpost.util;

import cpw.mods.fml.common.network.ByteBufUtils;
import gollorum.signpost.management.ConfigHandler;
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
	
	public Connection canConnectTo(BaseInfo inf){
		if(inf==null){
			return Connection.VALID;
		}
		if(ConfigHandler.deactivateTeleportation){
			return Connection.VALID;
		}
		if(!(ConfigHandler.interdimensional||(sameWorld(inf.pos) && sameDim(inf.pos)))){
			return Connection.WORLD;
		}
		if(ConfigHandler.maxDist>-1&&distance(inf.pos)>ConfigHandler.maxDist){
			return Connection.DIST;
		}
		return Connection.VALID;
	}

	public static enum Connection{VALID, WORLD, DIST}
	
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
			if(other.x!=this.x){
				return false;
			}else if(other.y!=this.y){
				return false;
			}else if(other.z!=this.z){
				return false;
			}else if(!sameWorld(other)){
				return false;
			}else if(!sameDim(other)){
				return false;
			}else return true;
	}
	
	public boolean sameWorld(BlockPos other){
		if(other.world.equals("")){
			other.world = this.world;
		}else if(this.world.equals("")){
			this.world = other.world;
		}else if(!this.world.equals(other.world)){
			return false;
		}
		return true;
	}
	
	public boolean sameDim(BlockPos other){
		if(other.dim==Integer.MIN_VALUE || this.dim == Integer.MIN_VALUE){
			other.dim = this.dim = Math.max(other.dim, this.dim);
		}else if(other.dim!=this.dim){
				return false;
		}
		return true;
	}
	
	public BlockPos update(BlockPos newPos){
		x = newPos.x;
		y = newPos.y;
		z = newPos.z;
		if(!(newPos.dim==Integer.MIN_VALUE)){
			dim = newPos.dim;
		}else{
			newPos.dim = dim;
		}
		if(!newPos.world.equals("")){
			world = newPos.world;
		}else{
			newPos.world = world;
		}
		return this;
	}
	
	public double distance(BlockPos other){
		int dx = this.x-other.x;
		int dy = this.y-other.y;
		int dz = this.z-other.z;
		return Math.sqrt(dx*dx+dy*dy+dz*dz);
	}

	@Override
	public String toString(){
		return world+": "+x+"|"+y+"|"+z+" in "+dim;
	}
}