package gollorum.signpost.items;

import gollorum.signpost.Signpost;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class PostWrench extends Item {
	
	public static final String NAME = "wrench";
	
	public PostWrench() {
		this.setUnlocalizedName(Signpost.MODID+":"+NAME);
		this.setRegistryName(Signpost.MODID+":item"+NAME);
	    this.setCreativeTab(CreativeTabs.TOOLS);
	}

}
