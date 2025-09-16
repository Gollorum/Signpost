package gollorum.signpost.minecraft.models.modelGeneration;

import org.joml.Vector2f;

import java.util.function.Function;

public class TextureArea {
    public final Vector2f from;
    public final Vector2f fromTo;
    public final Vector2f toFrom;
    public final Vector2f to;

    public TextureArea(TextureSegment u, TextureSegment v) {
        this.from = new Vector2f(u.from, v.from);
        this.fromTo = new Vector2f(u.from, v.to);
        this.toFrom = new Vector2f(u.to, v.from);
        this.to = new Vector2f(u.to, v.to);
    }

    public TextureArea(Vector2f from, Vector2f fromTo, Vector2f toFrom, Vector2f toTo) {
        this.from = from;
        this.fromTo = fromTo;
        this.toFrom = toFrom;
        this.to = toTo;
    }

    public TextureArea rotate(FaceRotation rotation) {
        return new TextureArea(
            rotate(from, rotation),
            rotate(fromTo, rotation),
            rotate(toFrom, rotation),
            rotate(to, rotation)
        );
    }

    private static Vector2f rotate(Vector2f v, FaceRotation rotation) {
        var c = 8;
        var cc = c+c;
        return switch (rotation) {
            case Zero -> v;
            case CounterClockwise90 -> new Vector2f(v.y, cc - v.x);
            case UpsideDown -> new Vector2f(cc - v.x, cc - v.y);
            case Clockwise90 -> new Vector2f(cc - v.y, v.x);
            default -> throw new RuntimeException("Rotation type " + rotation + " is not supported");
        };
    }

    public TextureArea map(Function<Float, Float> uMapping, Function<Float, Float> vMapping) {
        return new TextureArea(
            new Vector2f(uMapping.apply(from.x), vMapping.apply(from.y)),
            new Vector2f(uMapping.apply(fromTo.x), vMapping.apply(fromTo.y)),
            new Vector2f(uMapping.apply(toFrom.x), vMapping.apply(toFrom.y)),
            new Vector2f(uMapping.apply(to.x), vMapping.apply(to.y))
        );
    }

    public TextureArea flipU() {
        return new TextureArea(
            new Vector2f(to.x, from.y),
            new Vector2f(toFrom.x, fromTo.y),
            new Vector2f(fromTo.x, toFrom.y),
            new Vector2f(from.x, to.y)
        );
    }

    public TextureArea flipV() {
        return new TextureArea(
            new Vector2f(from.x, to.y),
            new Vector2f(fromTo.x, toFrom.y),
            new Vector2f(toFrom.x, fromTo.y),
            new Vector2f(to.x, from.y)
        );
    }

}