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
	public ResourceLocation getDefaultBiomeTexture(Biome biome) {
		if(biome.getBiomeName().contains("Plains")){
			return PostPost.PostType.OAK.resLocMain;
		}else if(biome.getBiomeName().contains("Desert")){
			return new ResourceLocation("textures/blocks/cobblestone.png");
		}else if(biome.getBiomeName().contains("Taiga")){
			return PostPost.PostType.SPRUCE.resLocMain;
		}else if(biome.getBiomeName().contains("Savanna")){
			return PostPost.PostType.ACACIA.resLocMain;
		}else{
			return PostPost.PostType.OAK.resLocMain;
		}
	}
	
	public void setTextureToBiomeDefault(Biome biome){
		setTexture(getDefaultBiomeTexture(biome));
	}
}
