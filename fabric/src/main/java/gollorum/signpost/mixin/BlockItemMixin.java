package gollorum.signpost.mixin;

import gollorum.signpost.registry.BlockEventListener;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    @Inject(method = "place", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/item/BlockItem;getPlacementState(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;", shift = At.Shift.AFTER), cancellable = true)
    public void injectPlacement(BlockPlaceContext blockPlaceContext, CallbackInfoReturnable<InteractionResult> cir) {
        // TODO DS: This probably only happens on server -> notify client when aborting?
        if (!BlockEventListener.onBeforeBlockPlace((BlockItem) (Object) this, blockPlaceContext)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

}
