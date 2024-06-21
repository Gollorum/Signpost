package gollorum.signpost.mixin;

import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;
import java.util.BitSet;

@Mixin(ModelBlockRenderer.class)
public interface BlockModelRendererAccessor {

    @Invoker("calculateShape")
    void shapeCalculation(BlockAndTintGetter level, BlockState state, BlockPos pos, int[] vertices, Direction normal, @Nullable float[] aoFloats, BitSet bitset);
}
