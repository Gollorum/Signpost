package gollorum.signpost.utils;

import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;

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

    public static AngleProvider fetchFrom(CompoundTag tag, HolderLookup.Provider provider) {
        return CompoundSerializer.isContainedIn(tag)
            ? CompoundSerializer.decode(tag, provider)
            : new Literal(Angle.CompoundSerializer.decode(tag, provider));
    }
    public static final CompoundSerializable<AngleProvider> CompoundSerializer = new CompoundSerializable<>() {
        @Override
        public void encode(CompoundTag compound, AngleProvider angleProvider, HolderLookup.Provider provider) {
            if (angleProvider instanceof Literal) {
                compound.putString("type", "literal");
                compound.put("angle", Angle.CompoundSerializer.encode(((Literal) angleProvider).angle, provider));
            } else if (angleProvider instanceof WaystoneTarget) {
                compound.putString("type", "waystone");
                compound.put("cachedAngle", Angle.CompoundSerializer.encode(angleProvider.get(), provider));
            } else throw new RuntimeException("Invalid angle provider type " + angleProvider.getClass());
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return compound.contains("type") || Angle.CompoundSerializer.isContainedIn(compound);
        }

        @Override
        public AngleProvider decode(CompoundTag compound, HolderLookup.Provider provider) {
            String type = compound.getString("type");
            if (type.equals("literal")) return new Literal(Angle.CompoundSerializer.decode(compound.getCompound("angle"), provider));
            else if (type.equals("waystone")) return new WaystoneTarget(
                Angle.CompoundSerializer.decode(compound.getCompound("cachedAngle"), provider)
            );
            else if(Angle.CompoundSerializer.isContainedIn(compound)) return new Literal(Angle.CompoundSerializer.decode(compound, provider));
            else throw new RuntimeException("Invalid angle provider type " + type);
        }
    };

    public static final BufferSerializable<AngleProvider> BufferSerializer = new BufferSerializable<>() {

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, AngleProvider angleProvider) {
            if (angleProvider instanceof Literal) {
                buffer.writeUtf("literal");
                Angle.BufferSerializer.encode(buffer, ((Literal) angleProvider).angle);
            } else if (angleProvider instanceof WaystoneTarget) {
                buffer.writeUtf("waystone");
                Angle.BufferSerializer.encode(buffer, angleProvider.get());
            } else throw new RuntimeException("Invalid angle provider type " + angleProvider.getClass());
        }

        @Override
        public AngleProvider decode(RegistryFriendlyByteBuf buffer) {
            return switch (buffer.readUtf()) {
                case "literal" -> new Literal(Angle.BufferSerializer.decode(buffer));
                case "waystone" -> new WaystoneTarget(Angle.BufferSerializer.decode(buffer));
                default -> throw new RuntimeException("Invalid angle provider type " + buffer.readUtf());
            };
        }

        @Override
        public Class<AngleProvider> getTargetClass() {
            return AngleProvider.class;
        }

    };
}
