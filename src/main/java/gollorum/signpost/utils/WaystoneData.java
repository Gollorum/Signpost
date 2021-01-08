package gollorum.signpost.utils;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

public class WaystoneData {

    public final WaystoneHandle handle;
    public final String name;
    public final WaystoneLocationData location;

    public WaystoneData(WaystoneHandle handle, String name, WaystoneLocationData location) {
        this.handle = handle;
        this.name = name;
        this.location = location;
    }

    public WaystoneData withName(String newName) { return new WaystoneData(handle, newName, location); }

    public static final Serializer SERIALIZER = new Serializer();

    public static final class Serializer implements CompoundSerializable<WaystoneData> {

        @Override
        public void writeTo(WaystoneData data, CompoundNBT compound, String keyPrefix) {
            WaystoneHandle.SERIALIZER.writeTo(data.handle, compound, keyPrefix + "Handle");
            compound.putString(keyPrefix + "Name", data.name);
            WaystoneLocationData.SERIALIZER.writeTo(data.location, compound, keyPrefix + "Location");
        }

        @Override
        public WaystoneData read(CompoundNBT compound, String keyPrefix) {
            return new WaystoneData(
                WaystoneHandle.SERIALIZER.read(compound, keyPrefix + "Handle"),
                compound.getString(keyPrefix + "Name"),
                WaystoneLocationData.SERIALIZER.read(compound, keyPrefix + "Location")
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
        }

        @Override
        public WaystoneData readFrom(PacketBuffer buffer) {
            return new WaystoneData(
                WaystoneHandle.SERIALIZER.readFrom(buffer),
                buffer.readString(32767),
                WaystoneLocationData.SERIALIZER.readFrom(buffer)
            );
        }
    }

}
