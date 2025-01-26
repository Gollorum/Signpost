package gollorum.signpost.minecraft.rendering;

import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;

public interface ModelFactory {

    void initialize(ModelBakery bakery);

    ModelBaker makeModelBaker(ModelBakery.TextureGetter textureMapper, ResourceLocation model);

}
