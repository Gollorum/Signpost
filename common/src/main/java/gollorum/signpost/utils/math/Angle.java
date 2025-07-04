package gollorum.signpost.utils.math;

import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

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

    public static final CompoundSerializable<Angle> CompoundSerializer = new SerializerImpl();
    public static final class SerializerImpl implements CompoundSerializable<Angle> {

        private static final String key = "Radians";

        @Override
        public void encode(CompoundTag compound, Angle angle, HolderLookup.Provider provider) {
            compound.putFloat(key, angle.radians);
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return compound.contains(key);
        }

        @Override
        public Angle decode(CompoundTag compound, HolderLookup.Provider provider) {
            return Angle.fromRadians(compound.getFloatOr(key, 0));
        }
    };

    public static final BufferSerializable<Angle> BufferSerializer = new BufferSerializable<Angle>() {

        @Override
        public Class<Angle> getTargetClass() {
            return Angle.class;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, Angle angle) {
            buffer.writeFloat(angle.radians);
        }

        @Override
        public Angle decode(RegistryFriendlyByteBuf buffer) {
            return Angle.fromRadians(buffer.readFloat());
        }
    };

}
