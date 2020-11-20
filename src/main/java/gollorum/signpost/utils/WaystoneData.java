package gollorum.signpost.utils;

import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

public class WaystoneData {

    public final String name;
    public final Vector3 localSpawnLocation;

    public WaystoneData(String name, Vector3 localSpawnLocation) {
        this.name = name;
        this.localSpawnLocation = localSpawnLocation;
    }

    public WaystoneData withName(String newName) { return new WaystoneData(newName, localSpawnLocation); }

    public static final Serializer SERIALIZER = new Serializer();

    public static final class Serializer implements CompoundSerializable<WaystoneData> {

        @Override
        public void writeTo(WaystoneData data, CompoundNBT compound, String keyPrefix) {
            compound.putString(keyPrefix + "Name", data.name);
            Vector3.SERIALIZER.writeTo(data.localSpawnLocation, compound, keyPrefix + "SpawnLocation");
        }

        @Override
        public WaystoneData read(CompoundNBT compound, String keyPrefix) {
            return new WaystoneData(
                compound.getString(keyPrefix + "Name"),
                Vector3.SERIALIZER.read(compound, keyPrefix + "SpawnLocation")
            );
        }

        @Override
        public void writeTo(WaystoneData data, PacketBuffer buffer) {
            buffer.writeString(data.name);
            Vector3.SERIALIZER.writeTo(data.localSpawnLocation, buffer);
        }

        @Override
        public WaystoneData readFrom(PacketBuffer buffer) {
            return new WaystoneData(
                buffer.readString(),
                Vector3.SERIALIZER.readFrom(buffer)
            );
        }
    }

}
