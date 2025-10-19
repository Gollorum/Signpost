package gollorum.signpost.mixin;

import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Biome.class)
public abstract class BiomeMixin {

    @Accessor("climateSettings")
    public abstract Object getClimateSettings();

}
