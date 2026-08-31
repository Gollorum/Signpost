package gollorum.signpost.mixin;

import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives a signpost the map colour of its model type rather than of its material.
 *
 * <p>A block's map colour comes from its {@link BlockBehaviour.Properties}, which is fixed at registration -
 * so with one block per material a spruce post would draw oak-brown on a map. This is the one property that
 * used to differ between the post types of a single material (everything else - hardness, sound, instrument,
 * tool - was identical), and Minecraft happens to ask for it with a level and a position, so it can be
 * answered from the block entity.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public class MapColorInjector {

    @Inject(method = "getMapColor", at = @At("HEAD"), cancellable = true)
    private void signpost$modelTypeMapColor(BlockGetter level, BlockPos pos, CallbackInfoReturnable<MapColor> cir) {
        if (((BlockBehaviour.BlockStateBase)(Object)this).getBlock() instanceof PostBlock
            && level.getBlockEntity(pos) instanceof PostTile tile)
            cir.setReturnValue(tile.modelType().mapColorOrDefault());
    }

}
