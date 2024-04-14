package gollorum.signpost.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(targets = "net.minecraft.client.renderer.block.ModelBlockRenderer$AmbientOcclusionFace")
public interface AmbientOcclusionFaceAccessor {

    @Coerce
    @Invoker("<init>()V")
    static Object create() {
        return null;
    }
}
