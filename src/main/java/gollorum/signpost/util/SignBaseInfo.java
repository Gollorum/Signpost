package gollorum.signpost.util;

import gollorum.signpost.blocks.PostPost;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;

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
		if(biome.getBiome().getBiomeName().contains("Plains")){
			return PostPost.PostType.OAK.resLocMain;
		}else if(biome.getBiome().getBiomeName().contains("Desert")){
			return new ResourceLocation("textures/blocks/cobblestone.png");
		}else if(biome.getBiome().getBiomeName().contains("Taiga")){
			return PostPost.PostType.SPRUCE.resLocMain;
		}else if(biome.getBiome().getBiomeName().contains("Savanna")){
			return PostPost.PostType.ACACIA.resLocMain;
		}else{
			return PostPost.PostType.OAK.resLocMain;
		}
	}
	
	public void setTextureToBiomeDefault(BiomeContainer biome){
		setTexture(getDefaultBiomeTexture(biome));
	}
}
