package gollorum.signpost.minecraft.gui;

import java.util.function.Function;

public class Colors {

    public static final int white = 0xffffff;
    public static final int grey = 0x999999;
    public static final int black = 0x000000;

    public static final int valid = white;
    public static final int validInactive = 0xdddddd;

    public static final int invalid = 0xff4444;
    public static final int invalidInactive = 0xdd6666;

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00ffffff) + alpha << 24;
    }
    public static int withRed(int color, int red) {
        return (color & 0xff00ffff) + red << 16;
    }
    public static int withGreen(int color, int green) {
        return (color & 0xffff00ff) + green << 8;
    }
    public static int withBlue(int color, int blue) {
        return (color & 0xffffff00) + blue;
    }

    public static int withAlpha(int color, Function<Integer, Integer> alphaMapping) {
        return withAlpha(color, alphaMapping.apply(getAlpha(color)));
    }
    public static int withRed(int color, Function<Integer, Integer> redMapping) {
        return withRed(color, redMapping.apply(getRed(color)));
    }
    public static int withGreen(int color, Function<Integer, Integer> greenMapping) {
        return withGreen(color, greenMapping.apply(getGreen(color)));
    }
    public static int withBlue(int color, Function<Integer, Integer> blueMapping) {
        return withBlue(color, blueMapping.apply(getBlue(color)));
    }

    public static int map(int color, Function<Integer, Integer> mapping) {
        return from(mapping.apply(getRed(color)), mapping.apply(getGreen(color)), mapping.apply(getBlue(color)));
    }

    public static int from(int red, int green, int blue) {
        return (red << 16) + (green << 8) + blue;
    }

    public static int getAlpha(int color) { return (color >>> 24) & 0xff; }
    public static int getRed(int color) { return (color >>> 16) & 0xff; }
    public static int getGreen(int color) { return (color >>> 8) & 0xff; }
    public static int getBlue(int color) { return color & 0xff; }

}
