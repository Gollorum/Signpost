package gollorum.signpost.items;

import gollorum.signpost.Signpost;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class PostBrush extends Item {
	
	public PostBrush() {
	    super();

	    this.setUnlocalizedName("SignpostBrush");
	    this.setCreativeTab(CreativeTabs.tabTools);
	    this.setTextureName(Signpost.MODID + ":brush");
	}

}
