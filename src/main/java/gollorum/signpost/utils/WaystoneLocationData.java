package gollorum.signpost.utils;

import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundNBT;

public class WaystoneLocationData {

    public final WorldLocation block;
    /// Global.
    public final Vector3 spawn;

    public WaystoneLocationData(WorldLocation block, Vector3 spawn) {
        this.block = block;
        this.spawn = spawn;
    }

    public static final Serializer SERIALIZER = new Serializer();

    public static class Serializer implements CompoundSerializable<WaystoneLocationData> {

        private Serializer() {}

        @Override
        public CompoundNBT write(WaystoneLocationData data, CompoundNBT compound) {
            compound.put("Block", WorldLocation.SERIALIZER.write(data.block));
            compound.put("Spawn", Vector3.Serializer.write(data.spawn));
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundNBT compound) {
            return compound.contains("Block") && WorldLocation.SERIALIZER.isContainedIn(compound.getCompound("Block"))
                && compound.contains("Spawn") && Vector3.Serializer.isContainedIn(compound.getCompound("Spawn"));
        }

        @Override
        public WaystoneLocationData read(CompoundNBT compound) {
            return new WaystoneLocationData(
                WorldLocation.SERIALIZER.read(compound.getCompound("Block")),
                Vector3.Serializer.read(compound.getCompound("Spawn"))
            );
        }
    }

}
