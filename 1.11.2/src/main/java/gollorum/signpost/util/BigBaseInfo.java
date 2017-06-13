package gollorum.signpost.util;

import net.minecraft.util.ResourceLocation;

public class BigBaseInfo {

	public Sign sign;
	public String[] description;
	public ResourceLocation postPaint;
	
	public BigBaseInfo(ResourceLocation texture){
		this(new Sign(texture), texture);
	}

	public BigBaseInfo(Sign sign, ResourceLocation texture){
		this.sign = sign;
		String[] description = {"Line 1", "Line 2", "Line 3", "Line 4"};
		this.description = description;
		this.postPaint = texture;
	}

	public BigBaseInfo(Sign sign, String[] description, ResourceLocation texture){
		this.sign = sign;
		this.description = description;
		this.postPaint = texture;
	}
}
