package gollorum.signpost.utils;

import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public interface AngleProvider {

    Angle get();

    public static final class Literal implements AngleProvider {
        private final Angle angle;

        public Literal(Angle angle) { this.angle = angle; }

        @Override
        public Angle get() { return angle; }
    }

    public static final class WaystoneTarget implements AngleProvider {

        private Angle cachedAngle;
        public void setCachedAngle(Angle cachedAngle) { this.cachedAngle = cachedAngle; }

        public WaystoneTarget(Angle cachedAngle) {
            this.cachedAngle = cachedAngle;
        }

        @Override
        public Angle get() { return cachedAngle; }
    }

    public static AngleProvider fetchFrom(CompoundTag tag) {
        return Serializer.isContainedIn(tag)
            ? Serializer.read(tag)
            : new Literal(Angle.Serializer.read(tag));
    }
    public static final CompoundSerializable<AngleProvider> Serializer = new CompoundSerializable<>() {
        @Override
        public Class<AngleProvider> getTargetClass() {
            return AngleProvider.class;
        }

        @Override
        public CompoundTag write(AngleProvider angleProvider, CompoundTag compound) {
            if (angleProvider instanceof Literal) {
                compound.putString("type", "literal");
                compound.put("angle", Angle.Serializer.write(((Literal) angleProvider).angle));
            } else if (angleProvider instanceof WaystoneTarget) {
                compound.putString("type", "waystone");
                compound.put("cachedAngle", Angle.Serializer.write(angleProvider.get()));
            } else throw new RuntimeException("Invalid angle provider type " + angleProvider.getClass());
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return compound.contains("type") || Angle.Serializer.isContainedIn(compound);
        }

        @Override
        public AngleProvider read(CompoundTag compound) {
            String type = compound.getString("type");
            if (type.equals("literal")) return new Literal(Angle.Serializer.read(compound.getCompound("angle")));
            else if (type.equals("waystone")) return new WaystoneTarget(
                Angle.Serializer.read(compound.getCompound("cachedAngle"))
            );
            else if(Angle.Serializer.isContainedIn(compound)) return new Literal(Angle.Serializer.read(compound));
            else throw new RuntimeException("Invalid angle provider type " + type);
        }
    };
}
