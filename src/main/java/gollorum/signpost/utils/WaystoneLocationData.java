package gollorum.signpost.utils;

import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundNBT;

public class WaystoneLocationData {

    public final WorldLocation block;
    public final Vector3 spawn;

    public WaystoneLocationData(WorldLocation block, Vector3 spawn) {
        this.block = block;
        this.spawn = spawn;
    }

    public static final Serializer SERIALIZER = new Serializer();

    public static class Serializer implements CompoundSerializable<WaystoneLocationData> {

        private Serializer() {}

        @Override
        public void writeTo(WaystoneLocationData data, CompoundNBT compound, String keyPrefix) {
            WorldLocation.SERIALIZER.writeTo(data.block, compound, keyPrefix + "Block");
            Vector3.SERIALIZER.writeTo(data.spawn, compound, keyPrefix + "Spawn");
        }

        @Override
        public boolean isContainedIn(CompoundNBT compound, String keyPrefix) {
            return WorldLocation.SERIALIZER.isContainedIn(compound, keyPrefix + "Block") &&
                Vector3.SERIALIZER.isContainedIn(compound, keyPrefix + "Spawn");
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
