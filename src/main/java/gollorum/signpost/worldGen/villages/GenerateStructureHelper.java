package gollorum.signpost.worldGen.villages;

import gollorum.signpost.util.MyBlockPos;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GenerateStructureHelper {

	private static final GenerateStructureHelper INSTANCE = new GenerateStructureHelper();
	
	public static GenerateStructureHelper getInstance(){
		return INSTANCE;
	}
	
	private GenerateStructureHelper(){}
	
	public BlockPos getTopSolidOrLiquidBlock(World world, BlockPos pos){
		BlockPos ret = world.getTopSolidOrLiquidBlock(pos);
		IBlockState state = world.getBlockState(ret);
		while(state.getMaterial().isLiquid()){
			ret = ret.add(0, 1, 0);
			state = world.getBlockState(ret);
		}
		return ret;
	}
    
	public MyBlockPos getTopSolidOrLiquidBlock(MyBlockPos pos) {
		BlockPos blockPos = getTopSolidOrLiquidBlock(pos.getWorld(), pos.toBlockPos());
		return new MyBlockPos(blockPos, pos.dim);
	}

}