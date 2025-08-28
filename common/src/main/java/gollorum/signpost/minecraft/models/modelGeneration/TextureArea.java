package gollorum.signpost.minecraft.models.modelGeneration;

import java.util.function.Function;

public class TextureArea {
    public final TextureSegment u;
    public final TextureSegment v;

    public TextureArea(TextureSegment u, TextureSegment v) {
        this.u = u;
        this.v = v;
    }

    public TextureArea rotate(FaceRotation rotation, boolean clampCoords) {
        return switch (rotation) {
            case Zero -> this;
            case Clockwise90 -> new TextureArea(
                new TextureSegment(v.from, v.to, clampCoords),
                new TextureSegment(16 - u.to, 16 - u.from, clampCoords)
            );
            case UpsideDown -> new TextureArea(
                new TextureSegment(u.to, u.from, clampCoords),
                new TextureSegment(v.to, v.from, clampCoords)
            );
            case CounterClockwise90 -> new TextureArea(
                new TextureSegment(16 - v.to, 16 - v.from, clampCoords),
                new TextureSegment(u.from, u.to, clampCoords)
            );
            default -> throw new RuntimeException("Rotation type " + rotation + " is not supported");
        };
    }

    public TextureArea map(Function<Float, Float> uMapping, Function<Float, Float> vMapping) {
        return new TextureArea(u.map(uMapping), v.map(vMapping));
    }

    public TextureArea flipU() {
        return new TextureArea(u.flip(), v);
    }

    public TextureArea flipV() {
        return new TextureArea(u, v.flip());
    }

}