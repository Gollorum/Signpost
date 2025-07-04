package gollorum.signpost.minecraft.rendering;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.SimpleModelWrapper;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class ModelFactory {

    protected ModelBakery bakery;

    public void initialize(ModelBakery bakery) {
        this.bakery = bakery;
    }

    public abstract ModelBaker makeModelBaker(SpriteGetter spriteGetter);

    public BlockModelPart bakeFor(Function<Material, TextureAtlasSprite> textureMapper, ResourceLocation model, ModelState modelState) {
        var baker = makeModelBaker(
            new SpriteGetter() {
                @Override
                public TextureAtlasSprite get(Material material, ModelDebugName modelDebugName) {
                    return textureMapper.apply(material);
                }

                @Override
                public TextureAtlasSprite reportMissingReference(String s, ModelDebugName modelDebugName) {
                    return ((AtlasSet.StitchResult) Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)).missing();
                }
            }
        );
        return SimpleModelWrapper.bake(baker, model, modelState);
    }
}
