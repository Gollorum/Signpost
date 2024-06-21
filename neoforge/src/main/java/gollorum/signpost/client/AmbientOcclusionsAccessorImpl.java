package gollorum.signpost.client;

import gollorum.signpost.minecraft.rendering.AmbientOcclusionsAccessor;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.BitSet;

public class AmbientOcclusionsAccessorImpl implements AmbientOcclusionsAccessor {

    private ModelBlockRenderer.AmbientOcclusionFace face = new ModelBlockRenderer.AmbientOcclusionFace();

    @Override
    public void calculate(BlockAndTintGetter level, BlockState state, BlockPos pos, Direction normal, float[] aoFloats, BitSet bitset, boolean isShaded) {
        face.calculate(level, state, pos, normal, aoFloats, bitset, isShaded);
    }

    @Override
    public float[] getBrightness() {
        return face.brightness;
    }

    @Override
    public int[] getLightMap() {
        return face.lightmap;
    }
}
