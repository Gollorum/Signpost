package gollorum.signpost.blocks;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;

public class ItemBlockWithMetaFacing extends ItemBlock{

	public ItemBlockWithMetaFacing(Block block){
		super(block);
		setMaxDamage(0);
		setHasSubtypes(true);
	}
	
}
