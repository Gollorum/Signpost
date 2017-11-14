package gollorum.signpost.items;

import gollorum.signpost.Signpost;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class CalibratedPostWrench extends Item {
	
	public CalibratedPostWrench() {
	    super();

	    this.setUnlocalizedName("SignpostCalibratedTool");
	    this.setCreativeTab(CreativeTabs.tabTools);
	    this.setTextureName(Signpost.MODID + ":toolcalibrated");
	}

}
