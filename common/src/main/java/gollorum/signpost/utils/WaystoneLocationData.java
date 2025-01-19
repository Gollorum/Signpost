package gollorum.signpost.utils;

import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class WaystoneLocationData {

    public final WorldLocation block;
    /// Global.
    public final Vector3 spawn;

    public WaystoneLocationData(WorldLocation block, Vector3 spawn) {
        this.block = block;
        this.spawn = spawn;
    }

    public WaystoneLocationData withoutExplicitLevel() {
        if(block.world.isLeft()) {
            return new WaystoneLocationData(new WorldLocation(block.blockPos, Either.right(block.world.leftOrThrow().dimension().location())), spawn);
        } else {
            return this;
        }
    }

    public static final CompoundSerializable<WaystoneLocationData> COMPOUND_SERIALIZER = new CompoundSerializable<WaystoneLocationData>() {

        @Override
        public void encode(CompoundTag compound, WaystoneLocationData data, HolderLookup.Provider provider) {
            compound.put("Block", WorldLocation.SERIALIZER.encode(data.block, provider));
            compound.put("Spawn", Vector3.CompoundSerializer.encode(data.spawn, provider));
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return compound.contains("Block") && WorldLocation.SERIALIZER.isContainedIn(compound.getCompound("Block"))
                && compound.contains("Spawn") && Vector3.CompoundSerializer.isContainedIn(compound.getCompound("Spawn"));
        }

        @Override
        public WaystoneLocationData decode(CompoundTag compound, HolderLookup.Provider provider) {
            return new WaystoneLocationData(
                WorldLocation.SERIALIZER.decode(compound.getCompound("Block"), provider),
                Vector3.CompoundSerializer.decode(compound.getCompound("Spawn"), provider)
            );
        }
    };

    public static final BufferSerializable<WaystoneLocationData> BUFFER_SERIALIZER = new BufferSerializable<>() {

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, WaystoneLocationData data) {
            WorldLocation.SERIALIZER.encode(buffer, data.block);
            Vector3.BufferSerializer.encode(buffer, data.spawn);
        }

        @Override
        public WaystoneLocationData decode(RegistryFriendlyByteBuf buffer) {
            return new WaystoneLocationData(
                WorldLocation.SERIALIZER.decode(buffer),
                Vector3.BufferSerializer.decode(buffer)
            );
        }

        @Override
        public Class<WaystoneLocationData> getTargetClass() {
            return WaystoneLocationData.class;
        }
    };

}
