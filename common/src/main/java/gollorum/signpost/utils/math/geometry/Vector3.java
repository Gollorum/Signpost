package gollorum.signpost.utils.math.geometry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.utils.math.Angle;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.function.Function;

public record Vector3(float x, float y, float z) {

    public static final Vector3 ZERO = new Vector3(0, 0, 0);

    public static Vector3 fromVec3d(Vec3 vec) {
        return new Vector3((float) vec.x, (float) vec.y, (float) vec.z);
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

    public static Vector3 fromBlockPos(BlockPos vec) {
        return new Vector3((float) vec.getX(), (float) vec.getY(), (float) vec.getZ());
    }

    public BlockPos toBlockPos() {
        return new BlockPos((int) x, (int) y, (int) z);
    }

    public static Vector3 min(Vector3 a, Vector3 b) {
        return new Vector3(Float.min(a.x, b.x), Float.min(a.y, b.y), Float.min(a.z, b.z));
    }

    public static Vector3 max(Vector3 a, Vector3 b) {
        return new Vector3(Float.max(a.x, b.x), Float.max(a.y, b.y), Float.max(a.z, b.z));
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

    public Vector3 div(Float other) {
        return new Vector3(x / other, y / other, z / other);
    }

    public Vector3 negated() {
        return new Vector3(-x, -y, -z);
    }


    public Vector3 mul(float f) {
        return new Vector3(x * f, y * f, z * f);
    }

    public Vector3 mul(Vector3 other) {
        return new Vector3(x * other.x, y * other.y, z * other.z);
    }

    public Vector3 rotateY(Angle angle) {
        return new Vector3(
            (float) (angle.cos() * x + angle.sin() * z),
            y,
            (float) (angle.sin() * x + angle.cos() * z)
        );
    }

    public float distanceTo(Vector3 other) {
        Vector3 distance = other.subtract(this);
        return (float) Math.sqrt(distance.x * distance.x + distance.y * distance.y + distance.z * distance.z);
    }

    public Vector3 map(Function<Float, Float> f) {
        return new Vector3(f.apply(x), f.apply(y), f.apply(z));
    }

    public Vector3 map(Vector3 b, Function<Float, Function<Float, Float>> f) {
        return new Vector3(
            f.apply(x).apply(b.x),
            f.apply(y).apply(b.y),
            f.apply(z).apply(b.z)
        );
    }

    public Vector3 map(Vector3 b, Vector3 c, Function<Float, Function<Float, Function<Float, Float>>> f) {
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

    public Vector3 withX(float x) {
        return new Vector3(x, y, z);
    }

    public Vector3 withX(Function<Float, Float> mapping) {
        return new Vector3(mapping.apply(x), y, z);
    }

    public Vector3 withY(float y) {
        return new Vector3(x, y, z);
    }

    public Vector3 withY(Function<Float, Float> mapping) {
        return new Vector3(x, mapping.apply(y), z);
    }

    public Vector3 withZ(float z) {
        return new Vector3(x, y, z);
    }

    public Vector3 withZ(Function<Float, Float> mapping) {
        return new Vector3(x, y, mapping.apply(z));
    }

    public Vector4 withW(float w) {
        return new Vector4(x, y, z, w);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vector3 vector3 = (Vector3) o;
        return Float.compare(vector3.x, x) == 0 &&
            Float.compare(vector3.y, y) == 0 &&
            Float.compare(vector3.z, z) == 0;
    }

    public Vector3 normalized() {
        float length = length();
        return new Vector3(x / length, y / length, z / length);
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public static final Codec<Vector3> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.FLOAT.fieldOf("x").forGetter(Vector3::x),
        Codec.FLOAT.fieldOf("y").forGetter(Vector3::y),
        Codec.FLOAT.fieldOf("z").forGetter(Vector3::z)
    ).apply(i, Vector3::new));

    public static final StreamCodec<ByteBuf, Vector3> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, Vector3::x,
        ByteBufCodecs.FLOAT, Vector3::y,
        ByteBufCodecs.FLOAT, Vector3::z,
        Vector3::new
    );
}
