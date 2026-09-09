package gollorum.signpost.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.minecraft.rendering.PostItemRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes the item display context reachable from a {@code SpecialModelRenderer}.
 *
 * <p>26.1 dropped the {@link ItemDisplayContext} parameter from
 * {@code SpecialModelRenderer#submit} - no vanilla renderer used it - but
 * {@link PostItemRenderer} does: it orients the post differently in the GUI, in hand and on the
 * ground, and picks a different render type for the GUI. The context is still known one frame
 * up, on the {@link ItemStackRenderState} that is about to submit the layer, so it is stashed
 * here for the duration of that call.
 *
 * <p>Rendering happens on the client render thread only, so a plain static is enough.
 */
@Mixin(ItemStackRenderState.class)
public class ItemStackRenderStateInjector {

    @Shadow
    ItemDisplayContext displayContext;

    @Inject(method = "submit", at = @At("HEAD"))
    private void signpost$captureDisplayContext(
        PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
        int lightCoords, int overlayCoords, int outlineColor, CallbackInfo ci
    ) {
        PostItemRenderer.currentDisplayContext = displayContext;
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void signpost$clearDisplayContext(
        PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
        int lightCoords, int overlayCoords, int outlineColor, CallbackInfo ci
    ) {
        PostItemRenderer.currentDisplayContext = ItemDisplayContext.NONE;
    }

}
