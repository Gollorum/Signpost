package gollorum.signpost.util;

import net.minecraft.util.ResourceLocation;

public class BigBaseInfo {

	public Sign sign;
	public String[] description;
	
	public BigBaseInfo(ResourceLocation texture){
		this(new Sign(texture));
	}

	public BigBaseInfo(Sign sign){
		this.sign = sign;
		String[] description = {"Line 1", "Line 2", "Line 3", "Line 4"};
		this.description = description;
	}

	public BigBaseInfo(Sign sign, String[] description){
		this.sign = sign;
		this.description = description;
	}
}
