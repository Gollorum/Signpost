package gollorum.signpost.minecraft.rendering;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiFunction;

public interface ModelFactory {

    void initialize(ModelBakery bakery);

    ModelBaker makeModelBaker(BiFunction<ResourceLocation, Material, TextureAtlasSprite> textureMapper, ResourceLocation model);

}
