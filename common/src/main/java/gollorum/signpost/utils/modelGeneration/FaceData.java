package gollorum.signpost.utils.modelGeneration;

import java.util.function.Function;

public record FaceData<TextureIdentifier>(TextureArea textureArea, FaceRotation rotation, TextureIdentifier texture, boolean shouldFlipNormal, int textureSize, int tintIndex) {

    public FaceData(TextureArea textureArea, FaceRotation rotation, TextureIdentifier texture, int textureSize, int tintIndex) {
        this(textureArea, rotation, texture, false, textureSize, tintIndex);
    }

    public FaceData<TextureIdentifier> withTextureArea(Function<TextureArea, TextureArea> mapping) {
        return new FaceData<>(mapping.apply(textureArea), rotation, texture, textureSize, tintIndex);
    }

    public FaceData<TextureIdentifier> withFlippedNormal() {
        return new FaceData<>(
            textureArea,
            rotation,
            texture,
            !shouldFlipNormal,
            textureSize,
            tintIndex);
    }

}
