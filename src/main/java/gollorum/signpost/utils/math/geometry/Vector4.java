package gollorum.signpost.utils.math.geometry;

import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Objects;
import java.util.function.Function;

public final class Vector4 {

    public static final Vector4 ZERO = new Vector4(0,0,0, 0);

    public final float x;
    public final float y;
    public final float z;
    public final float w;

    public Vector4(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4 add(Vector4 other) {
        return new Vector4(x + other.x, y + other.y, z + other.z, w + other.w);
    }

    public Vector4 subtract(Vector4 other) {
        return new Vector4(x - other.x, y - other.y, z - other.z, w - other.w);
    }

    public Vector4 mul(float f) {
        return new Vector4(x * f, y * f, z * f, w * f);
    }

    public Vector4 mul(Vector4 other) {
        return new Vector4(x * other.x, y * other.y, z * other.z, w * other.w);
    }

    public Vector4 mul(Matrix4x4 mat) {
        return new Vector4(
            mat.m00 * x + mat.m01 * y + mat.m02 * z + mat.m03 * w,
            mat.m10 * x + mat.m11 * y + mat.m12 * z + mat.m13 * w,
            mat.m20 * x + mat.m21 * y + mat.m22 * z + mat.m23 * w,
            mat.m30 * x + mat.m31 * y + mat.m32 * z + mat.m33 * w
        );
    }

    public Vector4 map(Function<Float, Float> f){
        return new Vector4(f.apply(x), f.apply(y), f.apply(z), f.apply(w));
    }

    public Vector4 map(Vector4 b, Function<Float, Function<Float, Float>> f){
        return new Vector4(
            f.apply(x).apply(b.x),
            f.apply(y).apply(b.y),
            f.apply(z).apply(b.z),
            f.apply(w).apply(b.w)
        );
    }

    public Vector4 map(Vector4 b, Vector4 c, Function<Float, Function<Float, Function<Float, Float>>> f){
        return new Vector4(
            f.apply(x).apply(b.x).apply(c.x),
            f.apply(y).apply(b.y).apply(c.y),
            f.apply(z).apply(b.z).apply(c.z),
            f.apply(w).apply(b.w).apply(c.w)
        );
    }

    public float max() { return Math.max(Math.max(Math.max(x, y), z), w); }

    public float min() { return Math.max(Math.min(Math.min(x, y), z), w); }

    public float dot(Vector4 v) { return x * v.x + y * v.y + z * v.z + w * v.w; }

    public Vector3 xyz() { return new Vector3(x, y, z); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vector4 Vector4 = (Vector4) o;
        return Float.compare(Vector4.x, x) == 0 &&
            Float.compare(Vector4.y, y) == 0 &&
            Float.compare(Vector4.z, z) == 0 &&
            Float.compare(Vector4.w, w) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, w);
    }

    public static final Serializer SERIALIZER = new Serializer();

    public static final class Serializer implements CompoundSerializable<Vector4> {

        private Serializer(){}

        @Override
        public CompoundTag write(Vector4 Vector4, CompoundTag compound) {
            compound.putFloat("X", Vector4.x);
            compound.putFloat("Y", Vector4.y);
            compound.putFloat("Z", Vector4.z);
            compound.putFloat("W", Vector4.w);
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return compound.contains("X") &&
                compound.contains("Y") &&
                compound.contains("Z") &&
                compound.contains("W");
        }

        @Override
        public Vector4 read(CompoundTag compound) {
            return new Vector4(
                compound.getFloat("X"),
                compound.getFloat("Y"),
                compound.getFloat("Z"),
                compound.getFloat("W")
            );
        }

        @Override
        public Class<Vector4> getTargetClass() {
            return Vector4.class;
        }

        @Override
        public void write(Vector4 vec, FriendlyByteBuf buffer) {
            buffer.writeFloat(vec.x);
            buffer.writeFloat(vec.y);
            buffer.writeFloat(vec.z);
            buffer.writeFloat(vec.w);
        }

        @Override
        public Vector4 read(FriendlyByteBuf buffer) {
            return new Vector4(
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat()
            );
        }
    }

}
