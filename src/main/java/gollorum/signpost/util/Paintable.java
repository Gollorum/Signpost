package gollorum.signpost.util;

import gollorum.signpost.Signpost;
import gollorum.signpost.util.code.MinecraftDependent;
import net.minecraft.util.ResourceLocation;

@MinecraftDependent
public interface Paintable {

	public static final ResourceLocation SIGN_PAINT = new ResourceLocation(Signpost.MODID, "textures/blocks/sign_paint.png");
	public static final ResourceLocation BIGSIGN_PAINT = new ResourceLocation(Signpost.MODID, "textures/blocks/bigsign_paint.png");
	public static final ResourceLocation POST_PAINT = new ResourceLocation(Signpost.MODID, "textures/blocks/paint.png");

	public ResourceLocation getTexture();
	public void setTexture(ResourceLocation texture);
	public ResourceLocation getDefaultBiomeTexture(BiomeContainer biome);
	public void setTextureToBiomeDefault(BiomeContainer biome);
	
}
