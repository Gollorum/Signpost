package gollorum.signpost.utils;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

public class WaystoneData {

    public final WaystoneHandle handle;
    public final String name;
    public final WaystoneLocationData location;
    public final OwnershipData ownership;

	public WaystoneData(WaystoneHandle handle, String name, WaystoneLocationData location, OwnershipData ownership) {
        this.handle = handle;
        this.name = name;
        this.location = location;
        this.ownership = ownership;
    }

    public WaystoneData withName(String newName) { return new WaystoneData(handle, newName, location, ownership); }

    public boolean hasThePermissionToEdit(PlayerEntity player) {
        return hasThePermissionToEdit(player, ownership);
    }

    public static boolean hasThePermissionToEdit(PlayerEntity player, OwnershipData ownership) {
        return !ownership.isLocked
            || ownership.owner.id.equals(player.getUniqueID())
            || player.hasPermissionLevel(Config.Server.permissions.editLockedWaystoneCommandPermissionLevel.get());
    }

    public static final Serializer SERIALIZER = new Serializer();

    public static final class Serializer implements CompoundSerializable<WaystoneData> {

        @Override
        public CompoundNBT write(WaystoneData data, CompoundNBT compound) {
            compound.put("Handle" , WaystoneHandle.Serializer.write(data.handle));
            compound.putString("Name", data.name);
            compound.put("Location", WaystoneLocationData.SERIALIZER.write(data.location));
            compound.put("Owner", OwnershipData.Serializer.write(data.ownership));
            return compound;
        }

        @Override
        public WaystoneData read(CompoundNBT compound) {
            return new WaystoneData(
                WaystoneHandle.Serializer.read(compound.getCompound("Handle")),
                compound.getString("Name"),
                WaystoneLocationData.SERIALIZER.read(compound.getCompound("Location")),
                OwnershipData.Serializer.read(compound.getCompound("Owner"))
            );
        }

        @Override
        public boolean isContainedIn(CompoundNBT compound) {
            return
                compound.contains("Handle") && WaystoneHandle.Serializer.isContainedIn(compound.getCompound("Handle")) &&
                compound.contains("Name") &&
                compound.contains("SpawnLocation") && WaystoneLocationData.SERIALIZER.isContainedIn(compound.getCompound("SpawnLocation"));
        }

        @Override
        public void write(WaystoneData data, PacketBuffer buffer) {
            WaystoneHandle.Serializer.write(data.handle, buffer);
            buffer.writeString(data.name);
            WaystoneLocationData.SERIALIZER.write(data.location, buffer);
            OwnershipData.Serializer.write(data.ownership, buffer);
        }

        @Override
        public WaystoneData read(PacketBuffer buffer) {
            return new WaystoneData(
                WaystoneHandle.Serializer.read(buffer),
                buffer.readString(32767),
                WaystoneLocationData.SERIALIZER.read(buffer),
                OwnershipData.Serializer.read(buffer)
            );
        }
    }

}
