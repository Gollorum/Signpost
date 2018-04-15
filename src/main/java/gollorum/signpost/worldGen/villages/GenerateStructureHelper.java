package gollorum.signpost.worldGen.villages;

import net.minecraft.block.Block;
import net.minecraft.world.World;

public class GenerateStructureHelper {

	private static final GenerateStructureHelper INSTANCE = new GenerateStructureHelper();
	
	public static GenerateStructureHelper getInstance(){
		return INSTANCE;
	}
	
	private GenerateStructureHelper(){}
	
	public int getTopSolidOrLiquidBlock(World world, int x, int z){
		int y = world.getTopSolidOrLiquidBlock(x, z);
		Block block = world.getBlock(x, y, z);
		while(block.getMaterial().isLiquid()){
			y += 1;
			block = world.getBlock(x, y, z);
		}
		return y;
	}

}