package gollorum.signpost.util;

import net.minecraft.util.ResourceLocation;

public class DoubleBaseInfo {

	public Sign sign1;
	public Sign sign2;


	public DoubleBaseInfo(ResourceLocation texture){
		this.sign1 = new Sign(texture);
		this.sign2 = new Sign(texture);
	}
	
	public DoubleBaseInfo(Sign sign1, Sign sign2){
		this.sign1 = sign1;
		this.sign2 = sign2;
	}
	
}
