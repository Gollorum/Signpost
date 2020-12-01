package gollorum.signpost.utils.math;

import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundNBT;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Angle angle = (Angle) o;
        return Float.compare(angle.radians, radians) == 0;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(radians);
    }

    public static final Serializer SERIALIZER = new Serializer();

    public static final class Serializer implements CompoundSerializable<Angle> {

        private static final String key = "Radians";

        private Serializer(){}

        @Override
        public void writeTo(Angle angle, CompoundNBT compound, String keyPrefix) {
            compound.putFloat(keyPrefix + key, angle.radians);
        }

        @Override
        public boolean isContainedIn(CompoundNBT compound, String keyPrefix) {
            return compound.contains(keyPrefix + key);
        }

        @Override
        public Angle read(CompoundNBT compound, String keyPrefix) {
            return Angle.fromRadians(compound.getFloat(keyPrefix + key));
        }
    }

}
