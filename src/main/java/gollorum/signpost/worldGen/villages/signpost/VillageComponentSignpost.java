package gollorum.signpost.worldGen.villages.signpost;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import gollorum.signpost.Signpost;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.code.MinecraftDependent;
import gollorum.signpost.worldGen.villages.GenerateStructureHelper;
import gollorum.signpost.worldGen.villages.VillageLibrary;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;

@MinecraftDependent
public class VillageComponentSignpost extends StructureVillagePieces.Village{
	
	private boolean built = false;
	private StructureVillagePieces.Start start;
	private EnumFacing facing;
	
	public VillageComponentSignpost(){
		super();
	}
		
	public VillageComponentSignpost(StructureVillagePieces.Start start, int type, StructureBoundingBox boundingBox, EnumFacing facing){
		super(start, type);
		this.boundingBox = boundingBox;
		this.start = start;
		this.facing = facing;
	}
	
	@Nullable
	public static StructureVillagePieces.Village buildComponent(StructureVillagePieces.Start startPiece, List<StructureComponent> pieces, Random random, int x, int y, int z, EnumFacing facing, int type) {
		StructureBoundingBox boundingBox = StructureBoundingBox.getComponentToAddBoundingBox(x, y, z, 0, 0, 0, 3, 3, 3, facing);
		if (canVillageGoDeeper(boundingBox) && findIntersecting(pieces, boundingBox) == null) {
			return new VillageComponentSignpost(startPiece, type, boundingBox, facing.getOpposite());
		}
		return null;
	}

	@Override
	public boolean addComponentParts(World world, Random random, StructureBoundingBox boundingBox) {
		if(built || start==null){
			return true;
		}else{
			built = true;
		}
		int x = (this.boundingBox.minX + this.boundingBox.maxX)/2;
		int z = (this.boundingBox.minZ + this.boundingBox.maxZ)/2;
		BlockPos postPos;
		try{
			postPos = GenerateStructureHelper.getInstance().getTopSolidOrLiquidBlock(world, new BlockPos(x, 0, z));
		}catch(Exception e) {
			postPos = new BlockPos(x, this.boundingBox.maxY, z);
		}
		world.setBlockState(postPos, Signpost.proxy.blockHandler.post_oak.getDefaultState());
		world.setBlockState(postPos.add(0, 1, 0), Signpost.proxy.blockHandler.post_oak.getDefaultState());
		if (world.getBlockState(postPos.add(0, -1, 0)).getMaterial().isLiquid()) {
			IBlockState block = this.getBiomeSpecificBlockState(Blocks.PLANKS.getDefaultState());
			world.setBlockState(postPos.add(0, -1, 0), block);
			world.setBlockState(postPos.add(-1, -1, -1), block);
			world.setBlockState(postPos.add(-1, -1, 0), block);
			world.setBlockState(postPos.add(-1, -1, 1), block);
			world.setBlockState(postPos.add(0, -1, -1), block);
			world.setBlockState(postPos.add(0, -1, 1), block);
			world.setBlockState(postPos.add(1, -1, -1), block);
			world.setBlockState(postPos.add(1, -1, 0), block);
			world.setBlockState(postPos.add(1, -1, 1), block);
		}
		StructureBoundingBox villageBox = start.getBoundingBox();
		MyBlockPos villagePos = new MyBlockPos(world, villageBox.minX, 0, villageBox.minZ);
		MyBlockPos blockPos = new MyBlockPos(world, postPos.add(0, 1, 0));
		VillageLibrary.getInstance().putSignpost(villagePos, blockPos, optimalRot(facing));
		return true;
	}

	private double optimalRot(EnumFacing facing) {
		switch(facing){
			case NORTH:
				return 0;
			case EAST:
				return 1.5*Math.PI;
			case SOUTH:
				return Math.PI;
			case WEST:
				return 0.5*Math.PI;
			default:
				return 0;
		}
	}
}
