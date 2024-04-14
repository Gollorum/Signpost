package gollorum.signpost.utils.math.geometry;

import gollorum.signpost.utils.math.Angle;

import java.util.function.Function;

public class Matrix4x4 {

    public static enum Axis {
        X, Y, Z
    }

    public static final Matrix4x4 ZERO = new Matrix4x4(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    public static final Matrix4x4 IDENTITY = new Matrix4x4(
        1, 0, 0, 0,
        0, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1
    );
    
    public static Matrix4x4 FromColumns(Vector4 c0, Vector4 c1, Vector4 c2, Vector4 c3) { return new Matrix4x4(
        c0.x, c1.x, c2.x, c3.x,
        c0.y, c1.y, c2.y, c3.y,
        c0.z, c1.z, c2.z, c3.z,
        c0.w, c1.w, c2.w, c3.w
    ); }

    public static Matrix4x4 FromRows(Vector4 r0, Vector4 r1, Vector4 r2, Vector4 r3) { return new Matrix4x4(
        r0.x, r0.y, r0.z, r0.w,
        r1.x, r1.y, r1.z, r1.w,
        r2.x, r2.y, r2.z, r2.w,
        r3.x, r3.y, r3.z, r3.w
    ); }

    public static Matrix4x4 translate(Vector3 v) { return new Matrix4x4(
        1, 0, 0, v.x,
        0, 1, 0, v.y,
        0, 0, 1, v.z,
        0, 0, 0, 1
    ); }

    public static Matrix4x4 rotateAround(Axis axis, Angle angle){
        float sin = (float) Math.sin(angle.radians());
        float cos = (float) Math.cos(angle.radians());
        switch (axis) {
            case X:
                return new Matrix4x4(
                    1, 0, 0, 0,
                    0, cos, -sin, 0,
                    0, sin, cos, 0,
                    0, 0, 0, 1
                );
            case Y:
                return new Matrix4x4(
                    cos, 0, sin, 0,
                    0, 1, 0, 0,
                    -sin, 0, cos, 0,
                    0, 0, 0, 1
                );
            case Z:
            default:
                return new Matrix4x4(
                    cos, -sin, 0, 0,
                    sin, cos, 0, 0,
                    0, 0, 1, 0,
                    0, 0, 0, 1
                );
        }
    }

    public static Matrix4x4 scale(Vector3 scale) { return new Matrix4x4(
        scale.x, 0, 0, 0,
        0, scale.y, 0, 0,
        0, 0, scale.z, 0,
        0, 0, 0, 1
    ); }

    public final float m00;
    public final float m01;
    public final float m02;
    public final float m03;
    public final float m10;
    public final float m11;
    public final float m12;
    public final float m13;
    public final float m20;
    public final float m21;
    public final float m22;
    public final float m23;
    public final float m30;
    public final float m31;
    public final float m32;
    public final float m33;

    public Matrix4x4(
        float m00, float m10, float m20, float m30,
        float m01, float m11, float m21, float m31,
        float m02, float m12, float m22, float m32,
        float m03, float m13, float m23, float m33
    ) {
        this.m00 = m00; this.m01 = m01; this.m02 = m02; this.m03 = m03;
        this.m10 = m10; this.m11 = m11; this.m12 = m12; this.m13 = m13;
        this.m20 = m20; this.m21 = m21; this.m22 = m22; this.m23 = m23;
        this.m30 = m30; this.m31 = m31; this.m32 = m32; this.m33 = m33;
    }

    public Vector4 getRow(int i){
        switch (i){
            case 0: return new Vector4(m00, m01, m02, m03);
            case 1: return new Vector4(m10, m11, m12, m13);
            case 2: return new Vector4(m20, m21, m22, m23);
            case 3: return new Vector4(m30, m31, m32, m33);
            default: throw new IndexOutOfBoundsException();
        }
    }

    public Vector4 getColumn(int i){
        switch (i){
            case 0: return new Vector4(m00, m10, m20, m30);
            case 1: return new Vector4(m01, m11, m21, m31);
            case 2: return new Vector4(m02, m12, m22, m32);
            case 3: return new Vector4(m30, m13, m23, m33);
            default: throw new IndexOutOfBoundsException();
        }
    }

    public Matrix4x4 transposed(){ return new Matrix4x4(
        m00, m01, m02, m03,
        m10, m11, m12, m13,
        m20, m21, m22, m23,
        m30, m31, m32, m33
    ); }

    public Matrix4x4 map(Function<Float, Float> f) { return new Matrix4x4(
        f.apply(m00), f.apply(m10), f.apply(m20), f.apply(m30),
        f.apply(m01), f.apply(m11), f.apply(m21), f.apply(m31),
        f.apply(m02), f.apply(m12), f.apply(m22), f.apply(m32),
        f.apply(m30), f.apply(m13), f.apply(m23), f.apply(m33)
    ); }
    
    public Matrix4x4 mapRows(Function<Vector4, Vector4> f) { return Matrix4x4.FromRows(
        f.apply(getRow(0)), f.apply(getRow(1)), f.apply(getRow(2)), f.apply(getRow(3))
    ); }

    public Vector4 mapRowsToVector(Function<Vector4, Float> f) { return new Vector4(
        f.apply(getRow(0)), f.apply(getRow(1)), f.apply(getRow(2)), f.apply(getRow(3))
    ); }

    public Matrix4x4 mapColumns(Function<Vector4, Vector4> f) { return Matrix4x4.FromColumns(
        f.apply(getColumn(0)), f.apply(getColumn(1)), f.apply(getColumn(2)), f.apply(getColumn(3))
    ); }

    public Vector4 mapColumnsToVector(Function<Vector4, Float> f) { return new Vector4(
        f.apply(getColumn(0)), f.apply(getColumn(1)), f.apply(getColumn(2)), f.apply(getColumn(3))
    ); }

    public Vector4 mul(Vector4 v) { return mapRowsToVector(r -> r.dot(v)); }

    public Matrix4x4 mul(Matrix4x4 m) {
        return Matrix4x4.FromColumns(
            mul(m.getColumn(0)),
            mul(m.getColumn(1)),
            mul(m.getColumn(2)),
            mul(m.getColumn(3))
        );
    }
}
