package gollorum.signpost.utils.math;

import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

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

    public static final CompoundSerializable<Angle> Serializer = new SerializerImpl();
    public static final class SerializerImpl implements CompoundSerializable<Angle> {

        @Override
        public Class<Angle> getTargetClass() {
            return Angle.class;
        }

        private static final String key = "Radians";

        @Override
        public CompoundTag write(Angle angle, CompoundTag compound) {
            compound.putFloat(key, angle.radians);
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return compound.contains(key);
        }

        @Override
        public Angle read(CompoundTag compound) {
            return Angle.fromRadians(compound.getFloat(key));
        }

        @Override
        public void write(Angle angle, FriendlyByteBuf buffer) {
            buffer.writeFloat(angle.radians);
        }

        @Override
        public Angle read(FriendlyByteBuf buffer) {
            return Angle.fromRadians(buffer.readFloat());
        }
    };

}
