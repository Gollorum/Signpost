package gollorum.signpost.services;

import gollorum.signpost.minecraft.worldgen.BiomeAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.biome.Biome;

public class BiomeAccessorImpl implements BiomeAccessor {
    @Override
    public float downfallIn(Biome biome) {
        var dynamicOps = NbtOps.INSTANCE;
        var encoded = Biome.NETWORK_CODEC.encodeStart(dynamicOps, biome);
        if (encoded.result().isPresent()) {
            var nbt = encoded.result().get();
            if (nbt instanceof CompoundTag tag) {
                return tag.getFloatOr("downfall", 0.0f);
            }
        }
        return 0.0f;
    }
}
