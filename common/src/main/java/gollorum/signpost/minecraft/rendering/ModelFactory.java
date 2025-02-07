package gollorum.signpost.minecraft.rendering;

import gollorum.signpost.mixin.ModelBakeryAccessor;
import gollorum.signpost.mixin.ModelBakeryLeecher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public abstract class ModelFactory {

    protected ModelBakery bakery;

    public void initialize(ModelBakery bakery) {
        this.bakery = bakery;
    }

    public abstract ModelBaker makeModelBaker(ModelBakery.TextureGetter textureMapper, ModelResourceLocation model);

    // TODO cache
    public BakedModel bakeFor(Function<Material, TextureAtlasSprite> textureMapper, ResourceLocation model) {
        return ((ModelBakeryAccessor) bakery).getUnbakedModels().get(model)
            .bake(makeModelBaker((loc, mat) -> textureMapper.apply(mat), new ModelResourceLocation(model, "")), textureMapper, new ModelState(){});
    }

}
