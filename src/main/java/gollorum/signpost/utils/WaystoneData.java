package gollorum.signpost.utils;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.OptionalSerializer;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

import java.util.Optional;

public class WaystoneData {

    public final WaystoneHandle handle;
    public final String name;
    public final WaystoneLocationData location;
    public final Optional<PlayerHandle> owner;

	public WaystoneData(WaystoneHandle handle, String name, WaystoneLocationData location, Optional<PlayerHandle> owner) {
        this.handle = handle;
        this.name = name;
        this.location = location;
        this.owner = owner;
    }

    public WaystoneData withName(String newName) { return new WaystoneData(handle, newName, location, owner); }

    public static final Serializer SERIALIZER = new Serializer();

    public static final class Serializer implements CompoundSerializable<WaystoneData> {

        @Override
        public void writeTo(WaystoneData data, CompoundNBT compound, String keyPrefix) {
            WaystoneHandle.SERIALIZER.writeTo(data.handle, compound, keyPrefix + "Handle");
            compound.putString(keyPrefix + "Name", data.name);
            WaystoneLocationData.SERIALIZER.writeTo(data.location, compound, keyPrefix + "Location");
            PlayerHandle.SERIALIZER.optional().writeTo(data.owner, compound, keyPrefix + "Owner");
        }

        @Override
        public WaystoneData read(CompoundNBT compound, String keyPrefix) {
            return new WaystoneData(
                WaystoneHandle.SERIALIZER.read(compound, keyPrefix + "Handle"),
                compound.getString(keyPrefix + "Name"),
                WaystoneLocationData.SERIALIZER.read(compound, keyPrefix + "Location"),
                PlayerHandle.SERIALIZER.optional().read(compound, keyPrefix + "Owner")
            );
        }

        @Override
        public boolean isContainedIn(CompoundNBT compound, String keyPrefix) {
            return
                WaystoneHandle.SERIALIZER.isContainedIn(compound, keyPrefix + "Handle") &&
                compound.contains(keyPrefix + "Name") &&
                WaystoneLocationData.SERIALIZER.isContainedIn(compound, keyPrefix + "SpawnLocation");
        }

        @Override
        public void writeTo(WaystoneData data, PacketBuffer buffer) {
            WaystoneHandle.SERIALIZER.writeTo(data.handle, buffer);
            buffer.writeString(data.name);
            WaystoneLocationData.SERIALIZER.writeTo(data.location, buffer);
            PlayerHandle.SERIALIZER.optional().writeTo(data.owner, buffer);
        }

        @Override
        public WaystoneData readFrom(PacketBuffer buffer) {
            return new WaystoneData(
                WaystoneHandle.SERIALIZER.readFrom(buffer),
                buffer.readString(32767),
                WaystoneLocationData.SERIALIZER.readFrom(buffer),
                PlayerHandle.SERIALIZER.optional().readFrom(buffer)
            );
        }
    }

}
