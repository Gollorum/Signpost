package gollorum.signpost.utils;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.StringSerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public class WaystoneData implements gollorum.signpost.WaystoneDataBase {

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

    public boolean hasThePermissionToEdit(Player player) {
        return hasThePermissionToEdit(player, location, isLocked);
    }

    public static boolean hasThePermissionToEdit(Player player, WaystoneLocationData locationData, boolean isLocked) {
        return !isLocked || hasSecurityPermissions(player, locationData);
    }

    public static boolean hasSecurityPermissions(Player player, WaystoneLocationData locationData) {
        return player.hasPermissions(Config.Server.permissions.editLockedWaystoneCommandPermissionLevel.get())
            || TileEntityUtils.toWorld(locationData.block.world, !(player instanceof ServerPlayer))
                .map(w -> w.getBlockEntity(locationData.block.blockPos))
                .flatMap(tile -> tile instanceof WithOwner.OfWaystone ? ((WithOwner.OfWaystone)tile).getWaystoneOwner() : Optional.empty())
                .map(owner -> owner.id.equals(player.getUUID()))
                .orElse(true);
    }

    public static final Serializer SERIALIZER = new Serializer();

    @Override
    public String name() {
        return name;
    }

    @Override
    public WaystoneLocationData loc() {
        return location;
    }

    @Override
    public WaystoneHandle handle() {
        return handle;
    }

    public static final class Serializer implements CompoundSerializable<WaystoneData> {

        @Override
        public CompoundTag write(WaystoneData data, CompoundTag compound) {
            compound.put("Handle" , WaystoneHandle.Vanilla.Serializer.write(data.handle));
            compound.putString("Name", data.name);
            compound.put("Location", WaystoneLocationData.SERIALIZER.write(data.location));
            compound.putBoolean("IsLocked", data.isLocked);
            return compound;
        }

        @Override
        public WaystoneData read(CompoundTag compound) {
            return new WaystoneData(
                WaystoneHandle.Vanilla.Serializer.read(compound.getCompound("Handle")),
                compound.getString("Name"),
                WaystoneLocationData.SERIALIZER.read(compound.getCompound("Location")),
                compound.getBoolean("IsLocked")
            );
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
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
        public void write(WaystoneData data, FriendlyByteBuf buffer) {
            WaystoneHandle.Vanilla.Serializer.write(data.handle, buffer);
            StringSerializer.instance.write(data.name, buffer);
            WaystoneLocationData.SERIALIZER.write(data.location, buffer);
            buffer.writeBoolean(data.isLocked);
        }

        @Override
        public WaystoneData read(FriendlyByteBuf buffer) {
            return new WaystoneData(
                WaystoneHandle.Vanilla.Serializer.read(buffer),
                StringSerializer.instance.read(buffer),
                WaystoneLocationData.SERIALIZER.read(buffer),
                buffer.readBoolean()
            );
        }
    }

}
