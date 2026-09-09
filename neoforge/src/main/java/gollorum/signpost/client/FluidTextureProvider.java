package gollorum.signpost.client;

import gollorum.signpost.minecraft.gui.utils.IFluidTextureProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

/**
 * 26.1 moved fluid textures and tints out of the loader's fluid-type extensions and into vanilla's
 * own {@code FluidModel}s, baked into the {@code FluidStateModelSet}. The loader API this used to
 * call ({@code IClientFluidTypeExtensions#getStillTexture} and friends) no longer exists, so the
 * models are read straight from the model manager - which mods register into too, so modded fluids
 * keep working.
 */
public class FluidTextureProvider implements IFluidTextureProvider {

    private static @Nullable FluidModel modelFor(Fluid fluid) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.getModelManager() == null) return null;
        return minecraft.getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
    }

    private static @Nullable Identifier nameOf(Material.@Nullable Baked material) {
        return material == null ? null : material.sprite().contents().name();
    }

    @Override
    public Identifier getStillTexture(Fluid fluid) {
        var model = modelFor(fluid);
        return model == null ? null : nameOf(model.stillMaterial());
    }

    @Override
    public Identifier getFlowingTexture(Fluid fluid) {
        var model = modelFor(fluid);
        return model == null ? null : nameOf(model.flowingMaterial());
    }

    @Override
    public Identifier getOverlayTexture(Fluid fluid) {
        var model = modelFor(fluid);
        return model == null ? null : nameOf(model.overlayMaterial());
    }

    @Override
    public int getTintColor(FluidState fluid, BlockAndTintGetter level, BlockPos pos) {
        var model = modelFor(fluid.getType());
        if (model == null || model.tintSource() == null) return -1;
        // The fluid tint sources ignore the block state; vanilla's water source only reads the biome.
        return model.tintSource().colorInWorld(Blocks.WATER.defaultBlockState(), level, pos);
    }
}
