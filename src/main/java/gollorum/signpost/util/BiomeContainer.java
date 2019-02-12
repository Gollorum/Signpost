package gollorum.signpost.util;

import gollorum.signpost.util.code.MinecraftDependent;
import net.minecraft.world.biome.Biome;

@MinecraftDependent
public class BiomeContainer {
	private Biome biome;

	public BiomeContainer(Biome biome) {
		this.biome = biome;
	}

	public Biome getBiome() {
		return biome;
	}
	
}
