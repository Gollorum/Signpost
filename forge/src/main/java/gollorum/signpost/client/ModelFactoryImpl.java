package gollorum.signpost.client;

import gollorum.signpost.minecraft.rendering.ModelFactory;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiFunction;

public class ModelFactoryImpl extends ModelFactory {

    @Override
    public ModelBaker makeModelBaker(ModelBakery.TextureGetter textureMapper, ModelResourceLocation model) {
        return bakery.new ModelBakerImpl(textureMapper, model);
    }
}
