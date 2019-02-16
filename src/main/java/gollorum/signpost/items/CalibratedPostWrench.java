package gollorum.signpost.items;

import gollorum.signpost.Signpost;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class CalibratedPostWrench extends Item {
	
	public static final String NAME = "wrench";
	
	public CalibratedPostWrench() {
	    this.setTranslationKey("SignpostCalibratedTool");
		this.setRegistryName(Signpost.MODID+":itemcalibratedwrench");
	    this.setCreativeTab(CreativeTabs.TOOLS);
	}

}
