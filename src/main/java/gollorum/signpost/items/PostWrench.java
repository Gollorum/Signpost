package gollorum.signpost.items;

import gollorum.signpost.Signpost;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class PostWrench extends Item {
	
	public PostWrench() {
	    super();

	    this.setUnlocalizedName("SignpostTool");
	    this.setCreativeTab(CreativeTabs.tabTools);
	    this.setTextureName(Signpost.MODID + ":tool");
	}

}
