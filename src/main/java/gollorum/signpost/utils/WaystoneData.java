package gollorum.signpost.utils;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.StringSerializer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

import java.util.Optional;

public class WaystoneData {

    public final WaystoneHandle.Vanilla handle;
    public final String name;
    public final WaystoneLocationData location;
    public final boolean isLocked;

	public WaystoneData(WaystoneHandle.Vanilla handle, String name, WaystoneLocationData location, boolean isLocked) {
        this.handle = handle;
        this.name = name;
        this.location = location;
        this.isLocked = isLocked;
    }

    public WaystoneData withName(String newName) { return new WaystoneData(handle, newName, location, isLocked); }

    public boolean hasThePermissionToEdit(PlayerEntity player) {
        return hasThePermissionToEdit(player, location, isLocked);
    }

    public static boolean hasThePermissionToEdit(PlayerEntity player, WaystoneLocationData locationData, boolean isLocked) {
        return !isLocked || hasSecurityPermissions(player, locationData);
    }

    public static boolean hasSecurityPermissions(PlayerEntity player, WaystoneLocationData locationData) {
        return player.hasPermissions(Config.Server.permissions.editLockedWaystoneCommandPermissionLevel.get())
            || TileEntityUtils.toWorld(locationData.block.world, !(player instanceof ServerPlayerEntity))
                .map(w -> w.getBlockEntity(locationData.block.blockPos))
                .flatMap(tile -> tile instanceof WithOwner.OfWaystone ? ((WithOwner.OfWaystone)tile).getWaystoneOwner() : Optional.empty())
                .map(owner -> owner.id.equals(player.getUUID()))
                .orElse(true);
    }

    public static final Serializer SERIALIZER = new Serializer();

    public static final class Serializer implements CompoundSerializable<WaystoneData> {

        @Override
        public CompoundNBT write(WaystoneData data, CompoundNBT compound) {
            compound.put("Handle" , WaystoneHandle.Vanilla.Serializer.write(data.handle));
            compound.putString("Name", data.name);
            compound.put("Location", WaystoneLocationData.SERIALIZER.write(data.location));
            compound.putBoolean("IsLocked", data.isLocked);
            return compound;
        }

        @Override
        public WaystoneData read(CompoundNBT compound) {
            return new WaystoneData(
                WaystoneHandle.Vanilla.Serializer.read(compound.getCompound("Handle")),
                compound.getString("Name"),
                WaystoneLocationData.SERIALIZER.read(compound.getCompound("Location")),
                compound.getBoolean("IsLocked")
            );
        }

        @Override
        public boolean isContainedIn(CompoundNBT compound) {
            return
                compound.contains("Handle") && WaystoneHandle.Vanilla.Serializer.isContainedIn(compound.getCompound("Handle")) &&
                compound.contains("Name") &&
                compound.contains("Location") && WaystoneLocationData.SERIALIZER.isContainedIn(compound.getCompound("Location")) &&
                compound.contains("IsLocked");
        }

        @Override
        public Class<WaystoneData> getTargetClass() {
            return WaystoneData.class;
        }

        @Override
        public void write(WaystoneData data, PacketBuffer buffer) {
            WaystoneHandle.Vanilla.Serializer.write(data.handle, buffer);
            StringSerializer.instance.write(data.name, buffer);
            WaystoneLocationData.SERIALIZER.write(data.location, buffer);
            buffer.writeBoolean(data.isLocked);
        }

        @Override
        public WaystoneData read(PacketBuffer buffer) {
            return new WaystoneData(
                WaystoneHandle.Vanilla.Serializer.read(buffer),
                StringSerializer.instance.read(buffer),
                WaystoneLocationData.SERIALIZER.read(buffer),
                buffer.readBoolean()
            );
        }
    }

}
