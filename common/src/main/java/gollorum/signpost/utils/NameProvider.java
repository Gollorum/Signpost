package gollorum.signpost.utils;

import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

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

    public static NameProvider fetchFrom(Tag tag, HolderLookup.Provider provider) {
        return tag instanceof CompoundTag && COMPOUND_SERIALIZER.isContainedIn((CompoundTag) tag)
            ? COMPOUND_SERIALIZER.decode((CompoundTag) tag, provider)
            : new Literal(tag.getAsString());
    }

    private static NameProvider from(String type, String name) {
        return switch (type) {
            case "literal" -> new Literal(name);
            case "waystone" -> new WaystoneTarget(name);
            default -> throw new RuntimeException("Invalid name provider type " + type);
        };
    }

    public static final CompoundSerializable<NameProvider> COMPOUND_SERIALIZER = new CompoundSerializable<>() {

        @Override
        public void encode(CompoundTag compound, NameProvider nameProvider, HolderLookup.Provider provider) {
            compound.putString("name", nameProvider.get());
            compound.putString("type", nameProvider instanceof Literal ? "literal" : "waystone");
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return compound.contains("name") && compound.contains("type");
        }

        @Override
        public NameProvider decode(CompoundTag compound, HolderLookup.Provider provider) {
            String type = compound.getString("type");
            String name = compound.getString("name");
            return from(type, name);
        }
    };

    public static final BufferSerializable<NameProvider> BUFFER_SERIALIZABLE = new BufferSerializable<>() {

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, NameProvider nameProvider) {
            buffer.writeUtf(nameProvider instanceof Literal ? "literal" : "waystone");
            buffer.writeUtf(nameProvider.get());
        }

        @Override
        public NameProvider decode(RegistryFriendlyByteBuf buffer) {
            return from(buffer.readUtf(), buffer.readUtf());
        }

        @Override
        public Class<NameProvider> getTargetClass() {
            return NameProvider.class;
        }

    };

}
