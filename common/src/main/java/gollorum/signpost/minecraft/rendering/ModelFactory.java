package gollorum.signpost.minecraft.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
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

    public BakedModel bakeFor(Function<Material, TextureAtlasSprite> textureMapper, ResourceLocation model) {
        return makeModelBaker(
            new ModelBakery.TextureGetter() {
                @Override
                public TextureAtlasSprite get(ModelDebugName modelDebugName, Material material) {
                    return textureMapper.apply(material);
                }

                @Override
                public TextureAtlasSprite reportMissingReference(ModelDebugName modelDebugName, String s) {
                    return ((AtlasSet.StitchResult) Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)).missing();
                }
            },
            new ModelResourceLocation(model, "")
        ).bake(model, new ModelState(){});
    }

}
