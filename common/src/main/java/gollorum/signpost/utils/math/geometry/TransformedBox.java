package gollorum.signpost.utils.math.geometry;

import gollorum.signpost.utils.math.Angle;

import java.util.Optional;

public class TransformedBox implements Intersectable<Ray, Float> {

    private final AABB axisAlignedBox;
    private final Matrix4x4 matrix;
    private final Matrix4x4 inverseMatrix;

    public TransformedBox(AABB axisAlignedBox, Matrix4x4 matrix, Matrix4x4 inverseMatrix) {
        this.axisAlignedBox = axisAlignedBox;
        this.matrix = matrix;
        this.inverseMatrix = inverseMatrix;
    }

    public TransformedBox(AABB axisAlignedBox) {
        this(axisAlignedBox, Matrix4x4.IDENTITY, Matrix4x4.IDENTITY);
    }

    public TransformedBox translate(Vector3 v) {
        return new TransformedBox(axisAlignedBox,
            matrix.mul(Matrix4x4.translate(v)),
            Matrix4x4.translate(v.negated()).mul(inverseMatrix)
        );
    }

    public TransformedBox rotateAlong(Matrix4x4.Axis axis, Angle angle){
        return new TransformedBox(axisAlignedBox,
            matrix.mul(Matrix4x4.rotateAround(axis, angle)),
            Matrix4x4.rotateAround(axis, angle.negated()).mul(inverseMatrix)
        );
    }

    public TransformedBox scale(Vector3 v) {
        return new TransformedBox(axisAlignedBox,
            matrix.mul(Matrix4x4.scale(v)),
            Matrix4x4.scale(v.map(f -> 1 / f)).mul(inverseMatrix)
        );
    }

    @Override
    public Optional<Float> IntersectWith(Ray ray) {
        return axisAlignedBox.IntersectWith(new Ray(
            transformPosition(ray.start, inverseMatrix),
            transformDirection(ray.dir, inverseMatrix)
        ));
    }

    @Override
    public AABB getBounds() {
        return axisAlignedBox.apply(v -> transformPosition(v, matrix));
    }

    private static Vector3 transformPosition(Vector3 v, Matrix4x4 mat){
        return
            mat.mul(
                v.subtract(new Vector3(0.5f, 0.5f, 0.5f)).withW(1)
            ).xyz()
            .add(new Vector3(0.5f, 0.5f, 0.5f));
    }

    private static Vector3 transformDirection(Vector3 v, Matrix4x4 mat){
        return mat.mul(v.withW(0)).xyz();
    }
}
