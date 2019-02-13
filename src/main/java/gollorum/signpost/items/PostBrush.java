package gollorum.signpost.items;

import gollorum.signpost.Signpost;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class PostBrush extends Item {
	
	public PostBrush() {
	    this.setTranslationKey("SignpostBrush");
		this.setRegistryName(Signpost.MODID+":itembrush");
	    this.setCreativeTab(CreativeTabs.TOOLS);
	}

}
