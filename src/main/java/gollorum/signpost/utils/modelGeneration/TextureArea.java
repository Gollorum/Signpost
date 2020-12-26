package gollorum.signpost.utils.modelGeneration;

import java.util.function.Function;

public class TextureArea {
    public final TextureSegment u;
    public final TextureSegment v;

    public TextureArea(TextureSegment u, TextureSegment v) {
        this.u = u;
        this.v = v;
    }

    public TextureArea rotate(FaceRotation rotation, boolean clampCoords) {
        switch (rotation) {
            case Zero:
                return this;
            case Clockwise90:
                return new TextureArea(v, new TextureSegment(u.to, u.from, clampCoords));
            case UpsideDown:
                return new TextureArea(new TextureSegment(u.to, u.from, clampCoords), new TextureSegment(v.to, v.from, clampCoords));
            case CounterClockwise90:
                return new TextureArea(new TextureSegment(v.to, v.from, clampCoords), u);
            default:
                throw new RuntimeException("Rotation type " + rotation + " is not supported");
        }
    }

    public TextureArea map(Function<Float, Float> uMapping, Function<Float, Float> vMapping) {
        return new TextureArea(u.map(uMapping), v.map(vMapping));
    }
}