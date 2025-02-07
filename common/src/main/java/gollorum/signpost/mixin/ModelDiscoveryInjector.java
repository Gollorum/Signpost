package gollorum.signpost.mixin;

import gollorum.signpost.minecraft.gui.PostModelResources;
import gollorum.signpost.minecraft.gui.WaystoneModelResources;
import net.minecraft.client.resources.model.ModelDiscovery;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelDiscovery.class)
public abstract class ModelDiscoveryInjector {

     @Inject(method = "discoverDependencies", at = @At("TAIL"))
     public void injectPostModelResources(CallbackInfo ci) {
         getBlockModel_invoke(WaystoneModelResources.inPostLocation);
         for(var loc : PostModelResources.all) getBlockModel_invoke(loc);
     }

     @Invoker("getBlockModel")
     protected abstract UnbakedModel getBlockModel_invoke(ResourceLocation location);
}