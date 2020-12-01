package gollorum.signpost.utils.math.geometry;

import com.sun.javafx.geom.Matrix3f;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;
import java.util.function.Function;

public final class Vector3 {

    public static final Vector3 ZERO = new Vector3(0,0,0);

    public static Vector3 fromVec3d(Vec3d vec){
        return new Vector3((float)vec.x, (float)vec.y, (float)vec.z);
    }

    public Vec3d asVec3d() {
        return new Vec3d(x, y, z);
    }

    public static Vector3 fromBlockPos(BlockPos vec){
        return new Vector3((float)vec.getX(), (float)vec.getY(), (float)vec.getZ());
    }

    public static Vector3 min(Vector3 a, Vector3 b) {
        return new Vector3(Float.min(a.x, b.x), Float.min(a.y, b.y), Float.min(a.z, b.z));
    }

    public static Vector3 max(Vector3 a, Vector3 b) {
        return new Vector3(Float.max(a.x, b.x), Float.max(a.y, b.y), Float.max(a.z, b.z));
    }

    public final float x;
    public final float y;
    public final float z;

    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3 add(Vector3 other) {
        return new Vector3(x + other.x, y + other.y, z + other.z);
    }
    public Vector3 subtract(Vector3 other) {
        return new Vector3(x - other.x, y - other.y, z - other.z);
    }
    public Vector3 div(Float other) { return new Vector3(x / other, y / other, z / other); }

    public Vector3 negated() { return new Vector3(-x, -y, -z); }


    public Vector3 mul(float f) {
        return new Vector3(x * f, y * f, z * f);
    }

    public Vector3 mul(Vector3 other) {
        return new Vector3(x * other.x, y * other.y, z * other.z);
    }

    public Vector3 mul(Matrix3f mat) {
        return new Vector3(
            mat.m00 * x + mat.m01 * y + mat.m02 * z,
            mat.m10 * x + mat.m11 * y + mat.m12 * z,
            mat.m20 * x + mat.m21 * y + mat.m22 * z
        );
    }

    public Vector3 map(Function<Float, Float> f){
        return new Vector3(f.apply(x), f.apply(y), f.apply(z));
    }

    public Vector3 map(Vector3 b, Function<Float, Function<Float, Float>> f){
        return new Vector3(
            f.apply(x).apply(b.x),
            f.apply(y).apply(b.y),
            f.apply(z).apply(b.z)
        );
    }

    public Vector3 map(Vector3 b, Vector3 c, Function<Float, Function<Float, Function<Float, Float>>> f){
        return new Vector3(
            f.apply(x).apply(b.x).apply(c.x),
            f.apply(y).apply(b.y).apply(c.y),
            f.apply(z).apply(b.z).apply(c.z)
        );
    }

    public float max() {
        return Math.max(Math.max(x, y), z);
    }

    public float min() {
        return Math.min(Math.min(x, y), z);
    }

    public Vector3 withX(float x) { return new Vector3(x, y, z); }
    public Vector3 withX(Function<Float, Float> mapping) { return new Vector3(mapping.apply(x), y, z); }
    public Vector3 withY(float y) { return new Vector3(x, y, z); }
    public Vector3 withY(Function<Float, Float> mapping) { return new Vector3(x, mapping.apply(y), z); }
    public Vector3 withZ(float z) { return new Vector3(x, y, z); }
    public Vector3 withZ(Function<Float, Float> mapping) { return new Vector3(x, y, mapping.apply(z)); }
    public Vector4 withW(float w) { return new Vector4(x, y, z, w); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vector3 vector3 = (Vector3) o;
        return Float.compare(vector3.x, x) == 0 &&
            Float.compare(vector3.y, y) == 0 &&
            Float.compare(vector3.z, z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    public static final Serializer SERIALIZER = new Serializer();

    public Vector3 normalized() {
        float length = length();
        return new Vector3(x / length, y / length, z / length);
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public static final class Serializer implements CompoundSerializable<Vector3> {

        private Serializer(){}

        @Override
        public void writeTo(Vector3 vector3, CompoundNBT compound, String keyPrefix) {
            compound.putFloat(keyPrefix + "X", vector3.x);
            compound.putFloat(keyPrefix + "Y", vector3.y);
            compound.putFloat(keyPrefix + "Z", vector3.z);
        }

        @Override
        public boolean isContainedIn(CompoundNBT compound, String keyPrefix) {
            return compound.contains(keyPrefix + "X") &&
                compound.contains(keyPrefix + "Y") &&
                compound.contains(keyPrefix + "Z");
        }

        @Override
        public Vector3 read(CompoundNBT compound, String keyPrefix) {
            return new Vector3(
                compound.getFloat(keyPrefix + "X"),
                compound.getFloat(keyPrefix + "Y"),
                compound.getFloat(keyPrefix + "Z")
            );
        }

        @Override
        public void writeTo(Vector3 vec, PacketBuffer buffer) {
            buffer.writeFloat(vec.x);
            buffer.writeFloat(vec.y);
            buffer.writeFloat(vec.z);
        }

        @Override
        public Vector3 readFrom(PacketBuffer buffer) {
            return new Vector3(
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat()
            );
        }
    }

}
