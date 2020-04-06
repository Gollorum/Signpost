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
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.Objects;
import java.util.function.Function;

public class MyBlockPos{

	private static final String VERSION = "1";
	
	public int x, y, z;
	public int dim;

	public MyBlockPos(World world, int x, int y, int z) {
		this(x, y, z, dim(world));
	}

	public MyBlockPos(World world, BlockPos pos) {
		this(pos, dim(world));
	}

	public MyBlockPos(BlockPos pos, int dim){
		x = pos.getX();
		y = pos.getY();
		z = pos.getZ();
		this.dim = dim;
	}
	
	public MyBlockPos(int x, int y, int z, int dim){
		this.x = x;
		this.y = y;
		this.z = z;
		this.dim = dim;
	}

	public MyBlockPos(MyBlockPos pos) {
		this(pos.x, pos.y, pos.z, pos.dim);
	}
	
	public MyBlockPos(Entity entity){
		this((int)Math.floor(entity.posX), (int)Math.floor(entity.posY), (int)Math.floor(entity.posZ), dim(entity.world));
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
		if(!checkInterdimensional(inf.teleportPosition)){
			return Connection.WORLD;
		}
		if(ClientConfigStorage.INSTANCE.getMaxDist()>-1&&distance(inf.teleportPosition)>ClientConfigStorage.INSTANCE.getMaxDist()){
			return Connection.DIST;
		}
		return Connection.VALID;
	}
	
	public boolean checkInterdimensional(MyBlockPos pos){
		if(pos==null){
			return true;
		}
		boolean config = ClientConfigStorage.INSTANCE.interdimensional();
		return config || sameDim(pos);
	}

	public NBTTagCompound writeToNBT(NBTTagCompound tC){
		int[] arr = {x, y, z, dim};
		tC.setIntArray("Position", arr);
		tC.setString("Version", VERSION);
		return tC;
	}
	
	public static MyBlockPos readFromNBT(NBTTagCompound tC){
		int[] arr = tC.getIntArray("Position");
		return new MyBlockPos(arr[0], arr[1], arr[2], arr[3]);
	}

	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, VERSION);
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
		buf.writeInt(dim);
	}
	
	public static MyBlockPos fromBytes(ByteBuf buf) {
		String savedVersion = ByteBufUtils.readUTF8String(buf);
		int x = buf.readInt();
		int y = buf.readInt();
		int z = buf.readInt();
		int dim = buf.readInt();
		if(!savedVersion.equals(VERSION)) ByteBufUtils.readUTF8String(buf);
	    return new MyBlockPos(x, y, z, dim);
	}

	@Override
	public String toString(){
		return x+"|"+y+"|"+z+" in "+dim;
	}


	@Override
	public boolean equals(Object obj){
		if(!(obj instanceof MyBlockPos)){
			return false;
		}
		MyBlockPos other = (MyBlockPos)obj;
		return other.x == this.x
			&& other.y == this.y
			&& other.z == this.z
			&& sameDim(other);
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y, z, dim);
	}

	public boolean sameDim(MyBlockPos other){
		if(other.dim==Integer.MIN_VALUE || this.dim == Integer.MIN_VALUE){
			other.dim = this.dim = Math.max(other.dim, this.dim);
		} else return other.dim == this.dim;
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
		return this;
	}
	
	public double distance(MyBlockPos other){
		int dx = this.x-other.x;
		int dy = this.y-other.y;
		int dz = this.z-other.z;
		return Math.sqrt(dx*dx + dy*dy + dz*dz);
	}

	public BlockPos toBlockPos(){
		return new BlockPos(x, y, z);
	}

	public World getWorld(){
		return Signpost.proxy.getWorld(this.dim);
	}

	public TileEntity getTile(){
		World world = getWorld();
		if(world!=null){
			TileEntity tile = world.getTileEntity(this.toBlockPos());
			if(tile instanceof PostPostTile){
				((PostPostTile) tile).getBases();
			} else if(tile instanceof BigPostPostTile){
				((BigPostPostTile) tile).getBases();
			}
			return tile;
		}else{
			return null;
		}
	}

	public MyBlockPos withX(int x) {
		return new MyBlockPos(x, y, z, dim);
	}

	public MyBlockPos withX(Function<Integer, Integer> xMap) {
		return this.withX(xMap.apply(x));
	}

	public MyBlockPos withY(int y) {
		return new MyBlockPos(x, y, z, dim);
	}

	public MyBlockPos withY(Function<Integer, Integer> yMap) {
		return this.withY(yMap.apply(y));
	}

	public MyBlockPos withZ(int z) {
		return new MyBlockPos(x, y, z, dim);
	}

	public MyBlockPos withZ(Function<Integer, Integer> zMap) {
		return this.withZ(zMap.apply(z));
	}

	public MyBlockPos getBelow() {
		return this.withY(y -> y - 1);
	}

	public MyBlockPos front(EnumFacing facing, int i) {
		return this
			.withX(x -> x + facing.getXOffset() * i)
			.withY(y -> y + facing.getYOffset() * i)
			.withZ(z -> z + facing.getZOffset() * i);
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