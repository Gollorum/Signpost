package gollorum.signpost.util;

import gollorum.signpost.Signpost;
import net.minecraft.util.ResourceLocation;

public class BigBaseInfo implements Paintable{

	public Sign sign;
	public String[] description;
	public ResourceLocation postPaint;
	public boolean awaitingPaint = false;
	public Paintable paintObject = null;
	
	public BigBaseInfo(ResourceLocation signTexture, ResourceLocation postTexture){
		this(new Sign(signTexture), postTexture);
	}

	public BigBaseInfo(Sign sign, ResourceLocation texture){
		this.sign = sign;
		String[] description = {"", "", "", ""};
		this.description = description;
		this.postPaint = texture;
	}

	public BigBaseInfo(Sign sign, String[] description, ResourceLocation texture){
		this.sign = sign;
		this.description = description;
		this.postPaint = texture;
	}

	@Override
	public ResourceLocation getTexture() {
		return postPaint;
	}

	@Override
	public void setTexture(ResourceLocation texture) {
		postPaint = texture;
	}
}
