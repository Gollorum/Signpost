package gollorum.signpost.utils.modelGeneration;

import java.util.function.Function;

public class FaceData<TextureIdentifier> {
    public final TextureArea textureArea;
    public final FaceRotation rotation;
    public final TextureIdentifier texture;
    public final boolean shouldFlipNormal;

    public FaceData(TextureArea textureArea, FaceRotation rotation, TextureIdentifier texture) {
        this.textureArea = textureArea;
        this.rotation = rotation;
        this.texture = texture;
        shouldFlipNormal = false;
    }

    public FaceData(TextureArea textureArea, FaceRotation rotation, TextureIdentifier texture, boolean shouldFlipNormal) {
        this.textureArea = textureArea;
        this.rotation = rotation;
        this.texture = texture;
        this.shouldFlipNormal = shouldFlipNormal;
    }

    public FaceData<TextureIdentifier> withTextureArea(Function<TextureArea, TextureArea> mapping) {
        return new FaceData<>(mapping.apply(textureArea), rotation, texture);
    }

    public FaceData<TextureIdentifier> withFlippedNormal() {
        return new FaceData<>(
            textureArea,
            rotation,
            texture,
            !shouldFlipNormal
        );
    }

}
