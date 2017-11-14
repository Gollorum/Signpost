package gollorum.signpost.util;

import gollorum.signpost.Signpost;
import net.minecraft.util.ResourceLocation;

public interface Paintable {

	public static final ResourceLocation SIGN_PAINT = new ResourceLocation(Signpost.MODID, "textures/blocks/sign_paint.png");
	public static final ResourceLocation BIGSIGN_PAINT = new ResourceLocation(Signpost.MODID, "textures/blocks/bigsign_paint.png");
	public static final ResourceLocation POST_PAINT = new ResourceLocation(Signpost.MODID, "textures/blocks/paint.png");

	public ResourceLocation getTexture();
	public void setTexture(ResourceLocation texture);
	
}
