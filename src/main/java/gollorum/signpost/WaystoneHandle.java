package gollorum.signpost;

import gollorum.signpost.relations.ExternalWaystoneLibrary;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.StringSerializer;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public interface WaystoneHandle {

    void write(PacketBuffer buffer);
    void write(CompoundNBT compound);

    static Optional<WaystoneHandle> read(PacketBuffer buffer) {
        String type = StringSerializer.instance.read(buffer);
        if(type.equals(Vanilla.typeTag)) return Optional.of(Vanilla.Serializer.read(buffer));
        else return ExternalWaystoneLibrary.getInstance().read(type, buffer);
    }

    static Optional<WaystoneHandle> read(CompoundNBT compound) {
        String type = compound.getString("type");
        if(type.equals(Vanilla.typeTag)) return Optional.of(Vanilla.Serializer.read(compound));
        else return ExternalWaystoneLibrary.getInstance().read(type, compound);
    }

    public static class Vanilla implements WaystoneHandle {
        public static final String typeTag = "vanilla";

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

        public static final CompoundSerializable<Vanilla> Serializer = new SerializerImpl();

        @Override
        public void write(PacketBuffer buffer) {
            buffer.writeUtf(typeTag);
            Serializer.write(this, buffer);
        }

        @Override
        public void write(CompoundNBT compound) {
            Serializer.write(this, compound);
        }

        public static final class SerializerImpl implements CompoundSerializable<Vanilla> {

            @Override
            public CompoundNBT write(Vanilla playerHandle, CompoundNBT compound) {
                compound.putString("type", typeTag);
                compound.putUUID("Id", playerHandle.id);
                return compound;
            }

            @Override
            public boolean isContainedIn(CompoundNBT compound) {
                return compound.contains("Id");
            }

            @Override
            public Vanilla read(CompoundNBT compound) {
                return new Vanilla(compound.getUUID("Id"));
            }

            @Override
            public Class<Vanilla> getTargetClass() {
                return Vanilla.class;
            }

            @Override
            public void write(Vanilla playerHandle, PacketBuffer buffer) {
                buffer.writeUUID(playerHandle.id);
            }

            @Override
            public Vanilla read(PacketBuffer buffer) {
                return new Vanilla(buffer.readUUID());
            }
        };

    }

}