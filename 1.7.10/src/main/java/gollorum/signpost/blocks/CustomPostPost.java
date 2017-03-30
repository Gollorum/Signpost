package gollorum.signpost.blocks;

import gollorum.signpost.blocks.PostPost.Hit;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class CustomPostPost extends PostPost {
	
	public CustomPostPost() {
		super();
		setBlockName("SignpostCustomPost");
	}

	@Override
	public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_) {
		CustomPostPostTile tile = new CustomPostPostTile();
		return tile;
	}
	
	@Override
	protected boolean doThingsWithItem(Item item, Hit hit, PostPostTile tile) {
		CustomPostPostTile customTile = (CustomPostPostTile)tile;
		if(hit.target == HitTarget.BASE1) {
			customTile.uMax1 = item.getIconFromDamage(0).getMaxU();
			customTile.uMin1 = item.getIconFromDamage(0).getMinU();
			customTile.vMax1 = item.getIconFromDamage(0).getMaxV();
			customTile.vMin1 = item.getIconFromDamage(0).getMinV();
		} else if(hit.target == HitTarget.BASE2) {
			customTile.uMax2 = item.getIconFromDamage(0).getMaxU();
			customTile.uMin2 = item.getIconFromDamage(0).getMinU();
			customTile.vMax2 = item.getIconFromDamage(0).getMaxV();
			customTile.vMin2 = item.getIconFromDamage(0).getMinV();
		}
		return true;
	}

}
