//package gollorum.signpost.mixin;
//
//import gollorum.signpost.minecraft.gui.PostModelResources;
//import gollorum.signpost.minecraft.gui.WaystoneModelResources;
//import net.minecraft.client.resources.model.ModelBakery;
//import net.minecraft.client.resources.model.UnbakedModel;
//import net.minecraft.resources.ResourceLocation;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Unique;
//import org.spongepowered.asm.mixin.gen.Accessor;
//import org.spongepowered.asm.mixin.gen.Invoker;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//import java.util.Map;
//
 // TODO: This
//@Mixin(ModelBakery.class)
//public abstract class ModelBakeryInjector {
//
//    @Inject(method = "<init>", at = @At("RETURN"))
//    private void injectPostModelResources(CallbackInfo ci) {
//        var unbakedCache = getUnbakedModels();
//        var topLevelModels = getTopModels();
//        load(WaystoneModelResources.inPostLocation, unbakedCache, topLevelModels);
//        for(var loc : PostModelResources.all) load(loc, unbakedCache, topLevelModels);
//    }
//
//    @Unique
//    private void load(ResourceLocation loc, Map<ResourceLocation, UnbakedModel> unbakedCache, Map<ResourceLocation, UnbakedModel> topLevelModels) {
//        UnbakedModel model = ((ModelBakery)(Object)this).getModel(loc);
//        unbakedCache.put(loc, model);
//        topLevelModels.put(loc, model);
//        model.resolveParents(((ModelBakery)(Object)this)::getModel);
//    }
//
//    @Accessor
//    public abstract Map<ResourceLocation, UnbakedModel> getUnbakedModels();
//
//    @Accessor
//    public abstract Map<ResourceLocation, UnbakedModel> getTopModels();
//
//    @Invoker("getModel")
//    private UnbakedModel sudoGetModel(ResourceLocation location) {
//
//    }
//
//}