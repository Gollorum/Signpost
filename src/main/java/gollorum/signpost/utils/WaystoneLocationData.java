package gollorum.signpost.utils;

import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundNBT;

public class WaystoneLocationData {

    public final WorldLocation blockLocation;
    public final Vector3 spawnPosition;

    public WaystoneLocationData(WorldLocation blockLocation, Vector3 spawnPosition) {
        this.blockLocation = blockLocation;
        this.spawnPosition = spawnPosition;
    }

    public static class Serializer implements CompoundSerializable<WaystoneLocationData> {

        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {}

        @Override
        public void writeTo(WaystoneLocationData data, CompoundNBT compound, String keyPrefix) {
            WorldLocation.SERIALIZER.writeTo(data.blockLocation, compound, keyPrefix + "Block");
            Vector3.SERIALIZER.writeTo(data.spawnPosition, compound, keyPrefix + "Spawn");
        }

        @Override
        public WaystoneLocationData read(CompoundNBT compound, String keyPrefix) {
            return new WaystoneLocationData(
                WorldLocation.SERIALIZER.read(compound, keyPrefix + "Block"),
                Vector3.SERIALIZER.read(compound, keyPrefix + "Spawn")
            );
        }
    }

}
