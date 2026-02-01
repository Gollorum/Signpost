package gollorum.signpost.minecraft.gui.utils;

import gollorum.signpost.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public interface IFluidTextureProvider {

    public static IFluidTextureProvider getInstance() {
        return Services.FLUID_TEXTURE_PROVIDER;
    }

    Identifier getStillTexture(Fluid fluid);
    Identifier getFlowingTexture(Fluid fluid); Identifier getOverlayTexture(Fluid fluid);

    int getTintColor(FluidState fluid, BlockAndTintGetter level, BlockPos pos);

}
