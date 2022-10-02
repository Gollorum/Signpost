package gollorum.signpost.utils;

import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

public interface NameProvider {

    String get();

    public static final class Literal implements NameProvider {

        private final String name;

        @Override
        public String get() { return name; }

        public Literal(String name) { this.name = name; }
    }

    public static final class WaystoneTarget implements NameProvider {

        private String cachedName;
        public void setCachedName(String name) { cachedName = name; }

        @Override
        public String get() { return cachedName; }

        public WaystoneTarget(String cachedName) { this.cachedName = cachedName; }
    }

    public static NameProvider fetchFrom(Tag tag) {
        return tag instanceof CompoundTag && Serializer.isContainedIn((CompoundTag) tag)
            ? Serializer.read((CompoundTag) tag)
            : new Literal(tag.getAsString());
    }
    public static final CompoundSerializable<NameProvider> Serializer = new CompoundSerializable<>() {

        @Override
        public CompoundTag write(NameProvider nameProvider, CompoundTag compound) {
            compound.putString("name", nameProvider.get());
            compound.putString("type", nameProvider instanceof Literal ? "literal" : "waystone");
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return compound.contains("name") && compound.contains("type");
        }

        @Override
        public NameProvider read(CompoundTag compound) {
            String type = compound.getString("type");
            String name = compound.getString("name");
            return from(type, name);
        }

        @Override
        public void write(NameProvider nameProvider, FriendlyByteBuf buffer) {
            buffer.writeUtf(nameProvider instanceof Literal ? "literal" : "waystone");
            buffer.writeUtf(nameProvider.get());
        }

        @Override
        public NameProvider read(FriendlyByteBuf buffer) {
            return from(buffer.readUtf(), buffer.readUtf());
        }

        private NameProvider from(String type, String name) {
            switch (type) {
                case "literal": return new Literal(name);
                case "waystone": return new WaystoneTarget(name);
                default: throw new RuntimeException("Invalid name provider type " + type);
            }
        }

        @Override
        public Class<NameProvider> getTargetClass() {
            return NameProvider.class;
        }

    };

}
