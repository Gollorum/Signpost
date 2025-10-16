package gollorum.signpost.utils.math;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class Angle {

    public static final float DEGREES_TO_RADIANS = (float) Math.PI / 180;
    public static final float RADIANS_TO_DEGREES = 1 / DEGREES_TO_RADIANS;

    public static Angle between(float x1, float z1, float x2, float z2) {
        return fromRadians((float) (Math.atan2(z2, x2) - Math.atan2(z1, x1)));
    }

    public static final Angle ZERO = new Angle(0);

    private final float radians;

    public static Angle fromRadians(float radians){
        return new Angle(radians);
    }

    public static Angle fromDegrees(float degrees){
        return new Angle(degrees * DEGREES_TO_RADIANS);
    }

    private Angle(float radians){
        this.radians = radians;
    }

    public Angle add(Angle other) { return Angle.fromRadians(radians + other.radians); }
    public Angle subtract(Angle other) { return Angle.fromRadians(radians - other.radians); }
    public Angle mul(Float other) { return Angle.fromRadians(radians * other); }
    public Angle div(Float other) { return Angle.fromRadians(radians / other); }

    public Angle negated() { return new Angle(-radians); }

    public float radians(){
        return radians;
    }

    public float degrees(){
        return radians * RADIANS_TO_DEGREES;
    }

    public Angle normalized() {
        float pi = (float) Math.PI;
        float r = radians % (2 * pi);
        if(r < -pi) r += 2 * pi;
        if(r > pi) r -= 2 * pi;
        return fromRadians(r);
    }

    public double cos() { return Math.cos(radians()); }
    public double sin() { return Math.sin(radians()); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Angle angle = (Angle) o;
        return Float.compare(angle.radians, radians) == 0;
    }

    public boolean isNearly(Angle other, Angle threshold) {
        return Math.abs(radians - other.radians) <= threshold.radians;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(radians);
    }

    public static final MapCodec<Angle> MAP_CODEC = Codec.FLOAT.fieldOf("Radians").xmap(Angle::fromRadians, Angle::radians);

    public static final Codec<Angle> CODEC = MAP_CODEC.codec();

    public static final StreamCodec<ByteBuf, Angle> STREAM_CODEC = ByteBufCodecs.FLOAT.map(
        Angle::fromRadians,
        Angle::radians
    );

}
