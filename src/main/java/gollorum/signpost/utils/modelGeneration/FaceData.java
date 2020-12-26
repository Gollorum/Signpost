package gollorum.signpost.utils.modelGeneration;

import java.util.function.Function;

public class FaceData<TextureIdentifier> {
    public final TextureArea textureArea;
    public final FaceRotation rotation;
    public final TextureIdentifier texture;

    public FaceData(TextureArea textureArea, FaceRotation rotation, TextureIdentifier texture) {
        this.textureArea = textureArea;
        this.rotation = rotation;
        this.texture = texture;
    }

    public FaceData(TextureArea textureArea, TextureIdentifier texture) {
        this.textureArea = textureArea;
        this.texture = texture;
        this.rotation = FaceRotation.Zero;
    }

    public FaceData<TextureIdentifier> withTextureArea(Function<TextureArea, TextureArea> mapping) {
        return new FaceData<>(mapping.apply(textureArea), rotation, texture);
    }

}
