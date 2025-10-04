package gollorum.signpost.minecraft.models.modelGeneration;

import java.util.function.Function;

public record FaceData<TextureIdentifier>(
    TextureArea textureArea,
    FaceRotation rotation,
    TextureIdentifier texture,
    int textureSize,
    int tintIndex
) {

    public FaceData<TextureIdentifier> withTextureArea(Function<TextureArea, TextureArea> mapping) {
        return new FaceData<>(mapping.apply(textureArea), rotation, texture, textureSize, tintIndex);
    }

    public FaceData<TextureIdentifier> withFlippedNormal() {
        return new FaceData<>(
            textureArea,
            rotation,
            texture,
            textureSize,
            tintIndex);
    }

}
