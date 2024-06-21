package gollorum.signpost.minecraft.rendering;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.BitSet;

public interface AmbientOcclusionsAccessor {

    void calculate(BlockAndTintGetter level, BlockState state, BlockPos pos, Direction normal, float[] aoFloats, BitSet bitset, boolean isShaded);

    float[] getBrightness();
    int[] getLightMap();

}
