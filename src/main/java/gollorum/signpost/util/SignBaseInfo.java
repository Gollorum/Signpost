package gollorum.signpost.util;

import gollorum.signpost.blocks.PostPost;
import net.minecraft.util.ResourceLocation;

public abstract class SignBaseInfo implements Paintable {

	public ResourceLocation postPaint;
	public boolean awaitingPaint = false;
	public Paintable paintObject = null;

	@Override
	public ResourceLocation getTexture() {
		return postPaint;
	}

	@Override
	public void setTexture(ResourceLocation texture) {
		postPaint = texture;
	}

	@Override
	public ResourceLocation getDefaultBiomeTexture(BiomeContainer biome) {
		if(biome == null || biome.getBiome() == null) {
			return PostPost.PostType.OAK.resLocMain;
		}
		if(biome.getBiome().getRegistryName().getPath().contains("plains")){
			return PostPost.PostType.OAK.resLocMain;
		}else if(biome.getBiome().getRegistryName().getPath().contains("desert")){
			return new ResourceLocation("textures/blocks/cobblestone.png");
		}else if(biome.getBiome().getRegistryName().getPath().contains("taiga")){
			return PostPost.PostType.SPRUCE.resLocMain;
		}else if(biome.getBiome().getRegistryName().getPath().contains("savanna")){
			return PostPost.PostType.ACACIA.resLocMain;
		}else{
			return PostPost.PostType.OAK.resLocMain;
		}
	}
	
	public void setTextureToBiomeDefault(BiomeContainer biome){
		setTexture(getDefaultBiomeTexture(biome));
	}
}
