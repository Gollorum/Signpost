package gollorum.signpost.client;

import gollorum.signpost.minecraft.rendering.ModelFactory;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiFunction;

public class ModelFactoryImpl implements ModelFactory {
    private ModelBakery bakery;

    @Override
    public void initialize(ModelBakery bakery) {
        this.bakery = bakery;
    }

    @Override
    public ModelBaker makeModelBaker(ModelBakery.TextureGetter textureMapper, ResourceLocation model) {
        return bakery.new ModelBakerImpl(textureMapper, model);
    }
}
