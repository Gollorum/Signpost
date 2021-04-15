package gollorum.signpost.minecraft.gui.utils;

import java.util.function.Function;

public class Point {

    public static final Point zero = new Point(0, 0);

    public final int x;
    public final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point add(Point other) { return new Point(x + other.x, y + other.y); }
    public Point subtract(Point other) { return new Point(x - other.x, y - other.y); }

    public Point add(int x, int y) { return new Point(this.x + x, this.y + y); }
    public Point subtract(int x, int y) { return new Point(this.x - x, this.y - y); }

    public Point mul(int waystoneBoxScale) {
        return new Point(x * waystoneBoxScale, y * waystoneBoxScale);
    }

    public static Point min(Point a, Point b) {
        return new Point(Math.min(a.x, b.x), Math.min(a.y, b.y));
    }

    public static Point max(Point a, Point b) {
        return new Point(Math.max(a.x, b.x), Math.max(a.y, b.y));
    }
    
    public Point withX(Function<Integer, Integer> mapping) { return new Point(mapping.apply(x), y); }
    public Point withY(Function<Integer, Integer> mapping) { return new Point(x, mapping.apply(y)); }

    public Point withX(int x) { return new Point(x, y); }
    public Point withY(int y) { return new Point(x, y); }

}
