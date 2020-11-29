package gollorum.signpost.utils.math;

public class MathUtils {

    public static float lerp(float from, float to, float progress) {
        return from + (to - from) * progress;
    }

    public static int lerp(int from, int to, float progress) {
        return (int)(from + (to - from) * progress);
    }

    public static int clamp(int value, int min, int max) {
        if(value < min) return min;
        if(value > max) return max;
        return value;
    }
}
