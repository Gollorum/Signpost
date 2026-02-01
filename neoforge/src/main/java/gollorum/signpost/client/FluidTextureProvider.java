package gollorum.signpost.client;

import gollorum.signpost.minecraft.gui.utils.IFluidTextureProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.Nullable;

public class FluidTextureProvider implements IFluidTextureProvider {
    @Override
    public Identifier getStillTexture(Fluid fluid) {
        return IClientFluidTypeExtensions.of(fluid).getStillTexture();
    }

    @Override
    public Identifier getFlowingTexture(Fluid fluid) {
        return IClientFluidTypeExtensions.of(fluid).getFlowingTexture();
    }

    @Override
    public Identifier getOverlayTexture(Fluid fluid) {
        return IClientFluidTypeExtensions.of(fluid).getOverlayTexture();
    }

    @Override
    public int getTintColor(FluidState fluid, BlockAndTintGetter level, BlockPos pos) {
        return IClientFluidTypeExtensions.of(fluid.getType()).getTintColor(fluid, level, pos);
    }
}
