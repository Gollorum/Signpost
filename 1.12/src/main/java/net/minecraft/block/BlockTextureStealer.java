package net.minecraft.block;

import java.util.Set;

import gollorum.signpost.util.collections.Lurchsauna;

public class BlockTextureStealer {

	public static final BlockTextureStealer INSTANCE = new BlockTextureStealer();
	
	public BlockTextureStealer(){}
	
	public Set<String> getTextureNames(Block block){
		Lurchsauna<String> ret = new Lurchsauna<String>();
		if(block == null){
			return ret;
		}
		for(int side=0; side<6; side++){
			for(int meta=0; meta<16; meta++){
				try{block.
					IIcon icon = block.getIcon(side, meta);
					if(icon != null){
						ret.add(icon.getIconName());
					}
				}catch(Exception e){}
			}
		}
		return ret;
	}
	
}