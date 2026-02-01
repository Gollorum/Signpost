package gollorum.signpost.client;

import gollorum.signpost.minecraft.gui.utils.IFluidTextureProvider;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import net.minecraft.world.level.material.WaterFluid;

public class FluidTextureProvider implements IFluidTextureProvider {
    @Override
    public Identifier getStillTexture(Fluid fluid) {
        return switch (fluid) {
            case WaterFluid waterFluid -> Identifier.withDefaultNamespace("block/water_still");
            case LavaFluid lavaFluid -> Identifier.withDefaultNamespace("block/lava_still");
            case null, default -> null;
        };
    }

    @Override
    public Identifier getFlowingTexture(Fluid fluid) {
        return switch (fluid) {
            case WaterFluid waterFluid -> Identifier.withDefaultNamespace("block/water_flow");
            case LavaFluid lavaFluid -> Identifier.withDefaultNamespace("block/lava_flow");
            case null, default -> null;
        };
    }

    @Override
    public Identifier getOverlayTexture(Fluid fluid) {
        return switch (fluid) {
            case WaterFluid waterFluid -> Identifier.withDefaultNamespace("block/water_overlay");
            case null, default -> null;
        };
    }

    @Override
    public int getTintColor(FluidState fluid, BlockAndTintGetter level, BlockPos pos) {
        return switch (fluid.getType()) {
            case WaterFluid waterFluid -> BiomeColors.getAverageWaterColor(level, pos) | -16777216;
            case null, default -> -1;
        };
    }
}
