package gollorum.signpost.minecraft.gui;

import java.util.function.Function;

public class Rect {

    public enum XAlignment {
        Left, Center, Right
    }

    public enum YAlignment {
        Top, Center, Bottom
    }

    public final Point point;
    public final int width;
    public final int height;

    public final TextureSize size;

    public Rect(Point referencePoint, TextureSize textureSize, XAlignment xAlignment, YAlignment yAlignment){
        width = textureSize.width;
        height = textureSize.height;
        point = new Point(
            xCoordinateFor(referencePoint.x, width, xAlignment),
            yCoordinateFor(referencePoint.y, height, yAlignment)
        );
        size = textureSize;
    }

    public Rect(Point referencePoint, int width, int height, XAlignment xAlignment, YAlignment yAlignment){
        this(referencePoint, new TextureSize(width, height), xAlignment, yAlignment);
    }

    public Rect(Point a, Point b) {
        int minX = Math.min(a.x, b.x);
        int minY = Math.min(a.y, b.y);
        int maxX = Math.max(a.x, b.x);
        int maxY = Math.max(a.y, b.y);
        width = maxX - minX;
        height = maxY - minY;
        point = new Point(minX, minY);
        size = new TextureSize(width, height);
    }

    public static int xCoordinateFor(int referenceX, int width, XAlignment alignment){
        switch (alignment){
            case Left: return referenceX;
            case Center: return referenceX - width / 2;
            case Right: return referenceX - width;
            default: throw new RuntimeException(String.format("Alignment %s not supported", alignment));
        }
    }

    public static int yCoordinateFor(int referenceY, int height, YAlignment alignment){
        switch (alignment){
            case Top: return referenceY;
            case Center: return referenceY - height / 2;
            case Bottom: return referenceY - height;
            default: throw new RuntimeException(String.format("Alignment %s not supported", alignment));
        }
    }

    public Rect(Point point, int width, int height) {
        this.point = point;
        this.width = width;
        this.height = height;
        size = new TextureSize(width, height);
    }

    public Point min() { return point; }
    public Point max() { return new Point(point.x + width, point.y + height); }
    public Point center() { return new Point(point.x + width / 2, point.y + height / 2); }
    public Point at(XAlignment xAlignment, YAlignment yAlignment) {
        int x, y;
        switch (xAlignment) {
            case Left: x = this.point.x;
                break;
            case Center: x = this.point.x + width / 2;
                break;
            case Right: x = this.point.x + width;
                break;
            default: throw new RuntimeException(String.format("Alignment %s not supported", xAlignment));
        }
        switch (yAlignment) {
            case Top: y = this.point.y;
                break;
            case Center: y = this.point.y + height / 2;
                break;
            case Bottom: y = this.point.y + height;
                break;
            default: throw new RuntimeException(String.format("Alignment %s not supported", yAlignment));
        }
        return new Point(x, y);
    }

    public Rect offset(Point minOffset, Point maxOffset) {
        return new Rect(min().add(minOffset), max().add(maxOffset));
    }

    public Rect withPoint(Point point) {
        return new Rect(point, width, height);
    }

    public Rect withPoint(Point point, XAlignment xAlignment, YAlignment yAlignment) {
        return new Rect(point, width, height, xAlignment, yAlignment);
    }

    public Rect withPoint(Function<Point, Point> mapping) {
        return new Rect(mapping.apply(point), width, height);
    }

    public Rect withSize(Function<Integer, Integer> widthMapping, Function<Integer, Integer> heightMapping){
        return new Rect(point, widthMapping.apply(width), heightMapping.apply(height));
    }

    public Rect withHeight(Function<Integer, Integer> heightMapping){
        return new Rect(point, width, heightMapping.apply(height));
    }

    public Rect scaleCenter(float scale) {
        Point center = center();
        return new Rect(center, (int)(width * scale), (int)(height * scale), XAlignment.Center, YAlignment.Center);
    }

}
