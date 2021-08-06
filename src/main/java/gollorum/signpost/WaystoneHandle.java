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

    public static final CompoundSerializable<WaystoneHandle> Serializer = new SerializerImpl();
    public static final class SerializerImpl implements CompoundSerializable<WaystoneHandle> {

        @Override
        public CompoundNBT write(WaystoneHandle playerHandle, CompoundNBT compound) {
            compound.putUniqueId("Id", playerHandle.id);
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundNBT compound) {
            return compound.contains("Id");
        }

        @Override
        public WaystoneHandle read(CompoundNBT compound) {
            return new WaystoneHandle(compound.getUniqueId("Id"));
        }

        @Override
        public Class<WaystoneHandle> getTargetClass() {
            return WaystoneHandle.class;
        }

        @Override
        public void write(WaystoneHandle playerHandle, PacketBuffer buffer) {
            buffer.writeUniqueId(playerHandle.id);
        }

        @Override
        public WaystoneHandle read(PacketBuffer buffer) {
            return new WaystoneHandle(buffer.readUniqueId());
        }
    };

}
