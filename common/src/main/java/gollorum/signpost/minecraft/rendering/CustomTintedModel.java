package gollorum.signpost.minecraft.rendering;

import com.google.common.collect.Lists;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record CustomTintedModel(
    BlockModelPart original,
    int[] tint
) implements BlockModelPart {

    @Override
    public List<BakedQuad> getQuads(@Nullable Direction direction) {
        return Lists.transform(original.getQuads(direction), quad ->
            new BakedQuad(
                quad.vertices(),
                tint[quad.tintIndex()],
                quad.direction(),
                quad.sprite(),
                quad.shade(),
                quad.lightEmission()));
    }

    @Override
    public boolean useAmbientOcclusion() {
        return original.useAmbientOcclusion();
    }

    @Override
    public TextureAtlasSprite particleIcon() {
        return original.particleIcon();
    }
}