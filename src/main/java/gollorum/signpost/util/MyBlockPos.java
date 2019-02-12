package gollorum.signpost.util;

import gollorum.signpost.Signpost;
import gollorum.signpost.blocks.tiles.BigPostPostTile;
import gollorum.signpost.blocks.tiles.PostPostTile;
import gollorum.signpost.management.ClientConfigStorage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class MyBlockPos{
	
	public int x, y, z;
	public String world;
	public int dim;
	public String modID = Signpost.MODID;

	public MyBlockPos(World world, BlockPos pos, int dim){
		this(world, pos.getX(), pos.getY(), pos.getZ(), dim);
	}
	
	public MyBlockPos(World world, BlockPos pos){
		this(world, pos.getX(), pos.getY(), pos.getZ());
	}
	
	public MyBlockPos(World world, int x, int y, int z){
		this((world==null||world.isRemote)?"":world.getWorldInfo().getWorldName(), x, y, z, dim(world));
	}
	
	public MyBlockPos(World world, int x, int y, int z, int dim){
		this((world==null||world.isRemote)?"":world.getWorldInfo().getWorldName(), x, y, z, dim);
	}
	
	public MyBlockPos(String world, double x, double y, double z, int dim){
		this(world, (int)x, (int)y, (int)z, dim);
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

	public MyBlockPos(String world, int x, int y, int z, int dim, String modID){
		this(world, x, y, z, dim);
		this.modID = modID;
	}

	public MyBlockPos(MyBlockPos pos) {
		this(pos.world, pos.x, pos.y, pos.z, pos.dim);
	}
	
	public MyBlockPos(Entity entity){
		this(entity.getEntityWorld(), (int)Math.floor(entity.posX), (int)Math.floor(entity.posY), (int)Math.floor(entity.posZ), dim(entity.getEntityWorld()));
	}

	public static int dim(World world){
		if(world==null||world.provider==null){
			return Integer.MIN_VALUE;
		}else
			return world.provider.getDimension();
	}

	public static enum Connection{VALID, WORLD, DIST}
	
	public Connection canConnectTo(BaseInfo inf){
		if(inf==null){
			return Connection.VALID;
		}
		if(ClientConfigStorage.INSTANCE.deactivateTeleportation()){
			return Connection.VALID;
		}
		if(!checkInterdimensional(inf.pos)){
			return Connection.WORLD;
		}
		if(ClientConfigStorage.INSTANCE.getMaxDist()>-1&&distance(inf.pos)>ClientConfigStorage.INSTANCE.getMaxDist()){
			return Connection.DIST;
		}
		return Connection.VALID;
	}
	
	public boolean checkInterdimensional(MyBlockPos pos){
		if(pos==null){
			return true;
		}
		boolean config = ClientConfigStorage.INSTANCE.interdimensional();
		return config || (sameWorld(pos) && sameDim(pos));
	}

	public NBTTagCompound writeToNBT(NBTTagCompound tC){
		int[] arr = {x, y, z, dim};
		tC.setIntArray("Position", arr);
		tC.setString("WorldName", world);
		tC.setString("modID", modID);
		return tC;
	}
	
	public static MyBlockPos readFromNBT(NBTTagCompound tC){
		int[] arr = tC.getIntArray("Position");
		return new MyBlockPos(tC.getString("WorldName"), arr[0], arr[1], arr[2], arr[3], tC.getString("modID"));
	}

	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, world);
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
		buf.writeInt(dim);
		ByteBufUtils.writeUTF8String(buf, modID);
	}
	
	public static MyBlockPos fromBytes(ByteBuf buf) {
		String world = ByteBufUtils.readUTF8String(buf);
		int x = buf.readInt();
		int y = buf.readInt();
		int z = buf.readInt();
		int dim = buf.readInt();
		String modID = ByteBufUtils.readUTF8String(buf);
		return new MyBlockPos(world, x, y, z, dim, modID); 
	}

	@Override
	public boolean equals(Object obj){
		if(!(obj instanceof MyBlockPos)){
			return false;
		}
		MyBlockPos other = (MyBlockPos)obj;
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
	
	public boolean sameWorld(MyBlockPos other){
		return sameWorld(other.world);
	}
	
	public boolean sameWorld(String world){
		if(world.equals("")){
			world = this.world;
		}else if(this.world.equals("")){
			this.world = world;
		}else if(!this.world.equals(world)){
			return false;
		}
		return true;
	}
	
	public boolean sameDim(MyBlockPos other){
		if(other.dim==Integer.MIN_VALUE || this.dim == Integer.MIN_VALUE){
			other.dim = this.dim = Math.max(other.dim, this.dim);
		}else if(other.dim!=this.dim){
				return false;
		}
		return true;
	}

	public MyBlockPos update(MyBlockPos newPos){
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
	
	public double distance(MyBlockPos other){
		int dx = this.x-other.x;
		int dy = this.y-other.y;
		int dz = this.z-other.z;
		return Math.sqrt(dx*dx+dy*dy+dz*dz);
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
	
	public World getWorld(){
		return Signpost.proxy.getWorld(this.world, this.dim);
	}
	
	public TileEntity getTile(){
		World world = getWorld();
		if(world!=null){
			TileEntity tile = world.getTileEntity(this.toBlockPos());
			if(tile instanceof PostPostTile){
				((PostPostTile) tile).getBases();
			}else if(tile instanceof PostPostTile){
				((BigPostPostTile) tile).getBases();
			}
			return tile;
		}else{
			return null;
		}
	} 

	public MyBlockPos fromNewPos(int x, int y, int z) {
		return new MyBlockPos(world, x, y, z, dim, modID);
	}

	public MyBlockPos getBelow() {
		return fromNewPos(x, y - 1, z);
	}

	public MyBlockPos front(EnumFacing facing, int i) {
		int newX = x + facing.getFrontOffsetX() * i;
		int newY = y + facing.getFrontOffsetY() * i;
		int newZ = z + facing.getFrontOffsetZ() * i;
		return fromNewPos(newX, newY, newZ);
	}

	public BiomeContainer getBiome() {
		World world = getWorld();
		if (world != null) {
			return new BiomeContainer(world.getBiome(toBlockPos()));
		} else {
			return null;
		}
	}
}