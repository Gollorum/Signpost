package gollorum.signpost.utils.modelGeneration;

import java.util.function.Function;

public class TextureSegment {
    public final float from;
    public final float to;

    public TextureSegment(float from, float to, boolean clampCoords) {
        if(clampCoords && !(isInTextureBounds(from) && isInTextureBounds(to))) {
            float originalFrom = from;
            float originalTo = to;
            float diff = to - from;
            from = from % 16;
            if(from < 0) from += 16;
            if(from == 0 && diff < 0) from = 16;
            to = from + diff;
            if(!isInTextureBounds(to))
                throw new RuntimeException("The coordinates cannot be clamped; they cut the boundary: (" + originalFrom +"|" + originalTo + ")");
        }
        this.from = from;
        this.to = to;
    }

    private static boolean isInTextureBounds(float i) {
        return i >= 0 && i <= 16;
    }

	public TextureSegment map(Function<Float, Float> mapping) {
        return new TextureSegment(mapping.apply(from), mapping.apply(to), false);
	}
}
