package gollorum.signpost.minecraft.gui.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

import java.util.function.Function;

public class Colors {

    /**
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#ffffff;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final int white = 0xffffff;

    /**
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#999999;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final int grey = 0x999999;

    /**
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#e0e0e0;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final int lightGrey = 0xe0e0e0;

    /**
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#707070;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final int darkGrey = 0x707070;

    /**
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#000000;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final int black = 0x000000;
    public static final int highlight = ChatFormatting.AQUA.getColor();

    /**
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#ffffff;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final int valid = white;

    /**
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#dddddd;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final int validInactive = 0xdddddd;


    /**
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#ff444444;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final int invalid = 0xff4444;

    /**
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#dd6666;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
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

    public static int from(int red, int green, int blue, int alpha) {
        return (alpha << 24) + (red << 16) + (green << 8) + blue;
    }

    public static int getAlpha(int color) { return (color >>> 24) & 0xff; }
    public static int getRed(int color) { return (color >>> 16) & 0xff; }
    public static int getGreen(int color) { return (color >>> 8) & 0xff; }
    public static int getBlue(int color) { return color & 0xff; }

    public static int mul(int c1, int c2) {
        return from(
            getGreen(c1) * getGreen(c2) / 0xff,
            getGreen(c1) * getGreen(c2) / 0xff,
            getBlue(c1) * getBlue(c2) / 0xff
        );
    }

    public static MutableComponent wrap(String text, int color) {
        return Component.literal(text)
            .withStyle(style -> style.withColor(TextColor.fromRgb(color)));
    }

    public static MutableComponent wrap(MutableComponent text, int color) {
        return text.withStyle(style -> style.withColor(TextColor.fromRgb(color)));
    }

}
