package gollorum.signpost.util;

import gollorum.signpost.util.code.MinecraftDependent;
import net.minecraft.world.biome.BiomeGenBase;

@MinecraftDependent
public class BiomeContainer {
	private BiomeGenBase biome;

	public BiomeContainer(BiomeGenBase biome) {
		this.biome = biome;
	}

	public BiomeGenBase getBiome() {
		return biome;
	}
	
}
