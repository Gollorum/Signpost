package gollorum.signpost.services;

import gollorum.signpost.minecraft.worldgen.BiomeAccessor;
import net.minecraft.world.level.biome.Biome;

public class BiomeAccessorImpl implements BiomeAccessor {
    @Override
    public float downfallIn(Biome biome) {
        return biome.getModifiedClimateSettings().downfall();
    }
}
