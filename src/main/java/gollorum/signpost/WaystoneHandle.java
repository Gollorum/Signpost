package gollorum.signpost;

import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

import java.util.Objects;
import java.util.UUID;

public class WaystoneHandle {
    public final UUID id;

    public WaystoneHandle(UUID id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WaystoneHandle that = (WaystoneHandle) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static final Serializer SERIALIZER  = new Serializer();

    public static class Serializer implements CompoundSerializable<WaystoneHandle> {

        private Serializer() {}

        @Override
        public void writeTo(WaystoneHandle playerHandle, CompoundNBT compound, String keyPrefix) {
            compound.putUniqueId(keyPrefix + "Id", playerHandle.id);
        }

        @Override
        public boolean isContainedIn(CompoundNBT compound, String keyPrefix) {
            return compound.contains(keyPrefix + "Id");
        }

        @Override
        public WaystoneHandle read(CompoundNBT compound, String keyPrefix) {
            return new WaystoneHandle(compound.getUniqueId(keyPrefix + "Id"));
        }

        @Override
        public void writeTo(WaystoneHandle playerHandle, PacketBuffer buffer) {
            buffer.writeUniqueId(playerHandle.id);
        }

        @Override
        public WaystoneHandle readFrom(PacketBuffer buffer) {
            return new WaystoneHandle(buffer.readUniqueId());
        }
    }

}
