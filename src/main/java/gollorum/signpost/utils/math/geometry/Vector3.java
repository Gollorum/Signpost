package gollorum.signpost.utils.math.geometry;

import com.mojang.math.Vector4f;
import gollorum.signpost.utils.math.Angle;
import com.mojang.math.Vector3d;
import com.mojang.math.Vector3f;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.function.Function;

public final class Vector3 {

    public static final Vector3 ZERO = new Vector3(0,0,0);

    public static Vector3 fromVec3d(Vec3 vec){
        return new Vector3((float)vec.x, (float)vec.y, (float)vec.z);
    }

    public static Vector3 fromVec3d(Vector3d vec){
        return new Vector3((float)vec.x, (float)vec.y, (float)vec.z);
    }

    public static Vector3 fromVector3f(Vector3f vec) {
        return new Vector3(vec.x(), vec.y(), vec.z());
    }

    public static Vector3 fromVector4f(Vector4f vec) {
        return new Vector3(vec.x(), vec.y(), vec.z());
    }

    public Vec3 asVec3() {
        return new Vec3(x, y, z);
    }

    public Vector3f asVec3f() {
        return new Vector3f(x, y, z);
    }

    public static Vector3 fromBlockPos(BlockPos vec){
        return new Vector3((float)vec.getX(), (float)vec.getY(), (float)vec.getZ());
    }

    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
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
    public Vector3 add(float otherX, float otherY, float otherZ) {
        return new Vector3(x + otherX, y + otherY, z + otherZ);
    }
    public Vector3 subtract(float otherX, float otherY, float otherZ) {
        return new Vector3(x - otherX, y - otherY, z - otherZ);
    }
    public Vector3 div(Float other) { return new Vector3(x / other, y / other, z / other); }

    public Vector3 negated() { return new Vector3(-x, -y, -z); }


    public Vector3 mul(float f) {
        return new Vector3(x * f, y * f, z * f);
    }

    public Vector3 mul(Vector3 other) {
        return new Vector3(x * other.x, y * other.y, z * other.z);
    }

//    public Vector3 mul(Matrix3f mat) {
//        return new Vector3(
//            mat.m00 * x + mat.m01 * y + mat.m02 * z,
//            mat.m10 * x + mat.m11 * y + mat.m12 * z,
//            mat.m20 * x + mat.m21 * y + mat.m22 * z
//        );
//    }

    public Vector3 rotateY(Angle angle) {
        return new Vector3(
            (float)(angle.cos() * x + angle.sin() * z),
            y,
            (float)(angle.sin() * x + angle.cos() * z)
        );
    }

    public float distanceTo(Vector3 other) {
        Vector3 distance = other.subtract(this);
        return (float) Math.sqrt(distance.x * distance.x + distance.y * distance.y + distance.z * distance.z);
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

    public Vector3 normalized() {
        float length = length();
        return new Vector3(x / length, y / length, z / length);
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public static final CompoundSerializable<Vector3> Serializer = new SerializerImpl();
    public static final class SerializerImpl implements CompoundSerializable<Vector3> {

        @Override
        public CompoundTag write(Vector3 vector3, CompoundTag compound) {
            compound.putFloat("X", vector3.x);
            compound.putFloat("Y", vector3.y);
            compound.putFloat("Z", vector3.z);
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return compound.contains("X")
                && compound.contains("Y")
                && compound.contains("Z");
        }

        @Override
        public Vector3 read(CompoundTag compound) {
            return new Vector3(
                compound.getFloat("X"),
                compound.getFloat("Y"),
                compound.getFloat("Z")
            );
        }

        @Override
        public Class<Vector3> getTargetClass() {
            return Vector3.class;
        }

        @Override
        public void write(Vector3 vec, FriendlyByteBuf buffer) {
            buffer.writeFloat(vec.x);
            buffer.writeFloat(vec.y);
            buffer.writeFloat(vec.z);
        }

        @Override
        public Vector3 read(FriendlyByteBuf buffer) {
            return new Vector3(
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat()
            );
        }
    };

}
