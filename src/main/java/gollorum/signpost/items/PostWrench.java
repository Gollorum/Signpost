package gollorum.signpost.items;

import gollorum.signpost.Signpost;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class PostWrench extends Item {
	
	public PostWrench() {
	    this.setUnlocalizedName("SignpostTool");
		this.setRegistryName(Signpost.MODID+":itemwrench");
	    this.setCreativeTab(CreativeTabs.TOOLS);
	}

}
