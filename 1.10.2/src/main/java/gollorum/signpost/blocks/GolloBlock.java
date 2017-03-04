package gollorum.signpost.blocks;

import gollorum.signpost.Signpost;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class GolloBlock extends BlockContainer {

	public final String NAME;
	
	public GolloBlock(Material materialIn, String name) {
		super(materialIn);
		NAME = name;
		this.setUnlocalizedName(Signpost.MODID+":"+NAME);
		this.setRegistryName(Signpost.MODID+":block"+NAME);
	}
	/*
	@Override
	public boolean hasTileEntity(){
		return true;
	}
	
	@Override
	public abstract TileEntity createTileEntity(World world, IBlockState state);
*/
}
