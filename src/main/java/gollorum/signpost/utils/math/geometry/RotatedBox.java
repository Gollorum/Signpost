package gollorum.signpost.utils.math.geometry;

import com.sun.javafx.geom.Matrix3f;
import gollorum.signpost.utils.math.Angle;

import java.util.Optional;

public class RotatedBox implements Intersectable<Ray, Float> {

    public static enum Axis {
        X, Y, Z
    }

    private final AABB axisAlignedBox;
    private final Matrix3f rotationMatrix;
    private final Matrix3f inverseRotationMatrix;

    public RotatedBox(AABB axisAlignedBox, Axis axis, Angle angle) {
        this.axisAlignedBox = axisAlignedBox;
        rotationMatrix = calculateMatrix(axis, angle.radians());
        inverseRotationMatrix = calculateMatrix(axis, -angle.radians());
    }

    private static Matrix3f calculateMatrix(Axis axis, float radians){
        float sin = (float) Math.sin(radians);
        float cos = (float) Math.cos(radians);
        switch (axis) {
            case X:
                return new Matrix3f(
                    1, 0, 0,
                    0, cos, -sin,
                    0, sin, cos
                );
            case Y:
                return new Matrix3f(
                    cos, 0, sin,
                    0, 1, 0,
                    -sin, 0, cos
                );
            case Z:
            default:
                return new Matrix3f(
                    cos, -sin, 0,
                    sin, cos, 0,
                    0, 0, 1
                );
        }
    }

    @Override
    public Optional<Float> IntersectWith(Ray ray) {
        return axisAlignedBox.IntersectWith(new Ray(
            rotatePosition(ray.start, inverseRotationMatrix),
            rotateDirection(ray.dir, inverseRotationMatrix)
        ));
    }

    @Override
    public AABB getBounds() {
        return axisAlignedBox.apply(v -> rotatePosition(v, rotationMatrix));
    }

    private static Vector3 rotatePosition(Vector3 v, Matrix3f mat){
        return v
            .subtract(new Vector3(0.5f, 0.5f, 0.5f))
            .mul(mat)
            .add(new Vector3(0.5f, 0.5f, 0.5f));
    }

    private static Vector3 rotateDirection(Vector3 v, Matrix3f mat){
        return v.mul(mat);
    }
}
