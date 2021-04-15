package gollorum.signpost.utils;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.entity.player.PlayerEntity;
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

    public boolean hasThePermissionToEdit(PlayerEntity player) {
        return !owner.isPresent()
            || owner.get().id.equals(player.getUniqueID())
            || !player.hasPermissionLevel(Config.Server.permissions.editLockedWaystoneCommandPermissionLevel.get());
    }

    public static boolean hasThePermissionToEdit(PlayerEntity player, Optional<PlayerHandle> owner) {
        return !owner.isPresent()
            || owner.get().id.equals(player.getUniqueID())
            || !player.hasPermissionLevel(Config.Server.permissions.editLockedWaystoneCommandPermissionLevel.get());
    }

    public static final Serializer SERIALIZER = new Serializer();

    public static final class Serializer implements CompoundSerializable<WaystoneData> {

        @Override
        public CompoundNBT write(WaystoneData data, CompoundNBT compound) {
            compound.put("Handle" , WaystoneHandle.Serializer.write(data.handle));
            compound.putString("Name", data.name);
            compound.put("Location", WaystoneLocationData.SERIALIZER.write(data.location));
            compound.put("Owner", PlayerHandle.Serializer.optional().write(data.owner));
            return compound;
        }

        @Override
        public WaystoneData read(CompoundNBT compound) {
            return new WaystoneData(
                WaystoneHandle.Serializer.read(compound.getCompound("Handle")),
                compound.getString("Name"),
                WaystoneLocationData.SERIALIZER.read(compound.getCompound("Location")),
                PlayerHandle.Serializer.optional().read(compound.getCompound("Owner"))
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
            PlayerHandle.Serializer.optional().write(data.owner, buffer);
        }

        @Override
        public WaystoneData read(PacketBuffer buffer) {
            return new WaystoneData(
                WaystoneHandle.Serializer.read(buffer),
                buffer.readString(32767),
                WaystoneLocationData.SERIALIZER.read(buffer),
                PlayerHandle.Serializer.optional().read(buffer)
            );
        }
    }

}
