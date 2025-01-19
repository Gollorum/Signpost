package gollorum.signpost;

import com.mojang.serialization.Codec;
import gollorum.signpost.compat.ExternalWaystoneLibrary;
import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.StringSerializer;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public interface WaystoneHandle {

    void write(RegistryFriendlyByteBuf buffer);
    void write(CompoundTag compound, HolderLookup.Provider provider);

    static Optional<WaystoneHandle> read(RegistryFriendlyByteBuf buffer) {
        String type = StringSerializer.Buffer.decode(buffer);
        if(type.equals(Vanilla.typeTag)) return Optional.of(Vanilla.BufferSerializer.decode(buffer));
        else return ExternalWaystoneLibrary.getInstance().read(type, buffer);
    }

    static Optional<WaystoneHandle> read(CompoundTag compound, HolderLookup.Provider provider) {
        String type = compound.getString("type");
        if(type.equals(Vanilla.typeTag)) return Optional.of(Vanilla.CompoundSerializer.decode(compound, provider));
        else return ExternalWaystoneLibrary.getInstance().read(type, compound);
    }

    public static class Vanilla implements WaystoneHandle {
        public static final String typeTag = "vanilla";
        public static final Vanilla NIL = new Vanilla(Util.NIL_UUID);

        public final UUID id;

        public Vanilla(UUID id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Vanilla that = (Vanilla) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        public static final CompoundSerializable<Vanilla> CompoundSerializer = new CompoundSerializerImpl();
        public static final BufferSerializable<Vanilla> BufferSerializer = new BufferSerializerImpl();
        public static final Codec<Vanilla> vanillaCodec = Codec.STRING.xmap(
            s -> new Vanilla(UUID.fromString(s)),
            v -> v.id.toString()
        );

        @Override
        public void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeUtf(typeTag);
            BufferSerializer.encode(buffer, this);
        }

        @Override
        public void write(CompoundTag compound, HolderLookup.Provider provider) {
            CompoundSerializer.encode(compound, this, provider);
        }

        private static final class CompoundSerializerImpl implements CompoundSerializable<Vanilla> {

            @Override
            public void encode(CompoundTag compound, Vanilla playerHandle, HolderLookup.Provider provider) {
                compound.putString("type", typeTag);
                compound.putUUID("Id", playerHandle.id);
            }

            @Override
            public boolean isContainedIn(CompoundTag compound) {
                return compound.contains("Id");
            }

            @Override
            public Vanilla decode(CompoundTag compound, HolderLookup.Provider provider) {
                return new Vanilla(compound.getUUID("Id"));
            }

        }

        private static final class BufferSerializerImpl implements BufferSerializable<Vanilla> {
            @Override
            public Class<Vanilla> getTargetClass() {
                return Vanilla.class;
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, Vanilla playerHandle) {
                buffer.writeUUID(playerHandle.id);
            }

            @Override
            public Vanilla decode(RegistryFriendlyByteBuf buffer) {
                return new Vanilla(buffer.readUUID());
            }
        };

    }

}