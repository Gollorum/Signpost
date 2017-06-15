package gollorum.signpost.util;

import net.minecraft.util.ResourceLocation;

public class DoubleBaseInfo {

	public Sign sign1;
	public Sign sign2;
	public ResourceLocation postPaint;

	public DoubleBaseInfo(ResourceLocation signTexture, ResourceLocation postTexture){
		this.sign1 = new Sign(signTexture);
		this.sign2 = new Sign(signTexture);
		postPaint = postTexture;
	}
	
	public DoubleBaseInfo(Sign sign1, Sign sign2, ResourceLocation texture){
		this.sign1 = sign1;
		this.sign2 = sign2;
		postPaint = texture;
	}
	
}
