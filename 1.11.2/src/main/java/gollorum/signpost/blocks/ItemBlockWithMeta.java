package gollorum.signpost.blocks;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;

public class ItemBlockWithMeta extends ItemBlock{

	public ItemBlockWithMeta(Block block) {
		super(block);
		setMaxDamage(0);
		setHasSubtypes(true);
	}
	
	@Override
	public int getMetadata(int damage){
		return damage;
	}

}
