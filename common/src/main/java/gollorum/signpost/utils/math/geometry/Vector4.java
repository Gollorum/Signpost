package gollorum.signpost.utils.math.geometry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.function.Function;

public record Vector4(float x, float y, float z, float w) {

    public static final Vector4 ZERO = new Vector4(0, 0, 0, 0);

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

    public Vector4 map(Function<Float, Float> f) {
        return new Vector4(f.apply(x), f.apply(y), f.apply(z), f.apply(w));
    }

    public Vector4 map(Vector4 b, Function<Float, Function<Float, Float>> f) {
        return new Vector4(
            f.apply(x).apply(b.x),
            f.apply(y).apply(b.y),
            f.apply(z).apply(b.z),
            f.apply(w).apply(b.w)
        );
    }

    public Vector4 map(Vector4 b, Vector4 c, Function<Float, Function<Float, Function<Float, Float>>> f) {
        return new Vector4(
            f.apply(x).apply(b.x).apply(c.x),
            f.apply(y).apply(b.y).apply(c.y),
            f.apply(z).apply(b.z).apply(c.z),
            f.apply(w).apply(b.w).apply(c.w)
        );
    }

    public float max() {
        return Math.max(Math.max(Math.max(x, y), z), w);
    }

    public float min() {
        return Math.max(Math.min(Math.min(x, y), z), w);
    }

    public float dot(Vector4 v) {
        return x * v.x + y * v.y + z * v.z + w * v.w;
    }

    public Vector3 xyz() {
        return new Vector3(x, y, z);
    }

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

    public static final Codec<Vector4> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.FLOAT.fieldOf("X").forGetter(Vector4::x),
        Codec.FLOAT.fieldOf("Y").forGetter(Vector4::y),
        Codec.FLOAT.fieldOf("Z").forGetter(Vector4::z),
        Codec.FLOAT.fieldOf("W").forGetter(Vector4::w)
    ).apply(i, Vector4::new));

    public static final StreamCodec<ByteBuf, Vector4> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, Vector4::x,
        ByteBufCodecs.FLOAT, Vector4::y,
        ByteBufCodecs.FLOAT, Vector4::z,
        ByteBufCodecs.FLOAT, Vector4::w,
        Vector4::new
    );
}
