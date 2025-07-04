package gollorum.signpost.client;

import gollorum.signpost.minecraft.rendering.ModelFactory;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiFunction;

public class ModelFactoryImpl extends ModelFactory {

    @Override
    public ModelBaker makeModelBaker(SpriteGetter spriteGetter) {
        return bakery.new ModelBakerImpl(spriteGetter);
    }
}
