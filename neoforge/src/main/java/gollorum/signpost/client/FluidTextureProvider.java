package gollorum.signpost.client;

import gollorum.signpost.minecraft.gui.utils.IFluidTextureProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.Nullable;

public class FluidTextureProvider implements IFluidTextureProvider {
    @Override
    public ResourceLocation getStillTexture(Fluid fluid) {
        return IClientFluidTypeExtensions.of(fluid).getStillTexture();
    }

    @Override
    public ResourceLocation getFlowingTexture(Fluid fluid) {
        return IClientFluidTypeExtensions.of(fluid).getFlowingTexture();
    }

    @Override
    public ResourceLocation getOverlayTexture(Fluid fluid) {
        return IClientFluidTypeExtensions.of(fluid).getOverlayTexture();
    }

    @Override
    public int getTintColor(FluidState fluid, BlockAndTintGetter level, BlockPos pos) {
        return IClientFluidTypeExtensions.of(fluid.getType()).getTintColor(fluid, level, pos);
    }
}
