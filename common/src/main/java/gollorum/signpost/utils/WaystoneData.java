package gollorum.signpost.utils;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.StringSerializer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
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

    public WaystoneData withoutExplicitLevel() {
        if(location.block.world.isLeft()) {
            return new WaystoneData(handle, name, new WaystoneLocationData(new WorldLocation(location.block.blockPos, Either.right(location.block.world.leftOrThrow().dimension().location())), location.spawn), isLocked);
        } else {
            return this;
        }
    }

    public WaystoneData withName(String newName) { return new WaystoneData(handle, newName, location, isLocked); }

    public boolean hasThePermissionToEdit(Player player) {
        return hasThePermissionToEdit(player, location, isLocked);
    }

    public static boolean hasThePermissionToEdit(Player player, WaystoneLocationData locationData, boolean isLocked) {
        return !isLocked || hasSecurityPermissions(player, locationData);
    }

    public static boolean hasSecurityPermissions(Player player, WaystoneLocationData locationData) {
        return player.hasPermissions(IConfig.IServer.getInstance().permissions().editLockedWaystoneCommandPermissionLevel())
            || TileEntityUtils.toWorld(locationData.block.world, !(player instanceof ServerPlayer))
                .map(w -> w.getBlockEntity(locationData.block.blockPos))
                .flatMap(tile -> tile instanceof WithOwner.OfWaystone ? ((WithOwner.OfWaystone)tile).getWaystoneOwner() : Optional.empty())
                .map(owner -> owner.id.equals(player.getUUID()))
                .orElse(true);
    }

    public static final CompoundSerializer COMPOUND_SERIALIZER = new CompoundSerializer();
    public static final BufferSerializer BUFFER_SERIALIZER = new BufferSerializer();

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

    public static final class CompoundSerializer implements CompoundSerializable<WaystoneData> {

        @Override
        public void encode(CompoundTag compound, WaystoneData data, HolderLookup.Provider provider) {
            compound.put("Handle" , WaystoneHandle.Vanilla.CompoundSerializer.encode(data.handle, provider));
            compound.putString("Name", data.name);
            compound.put("Location", WaystoneLocationData.COMPOUND_SERIALIZER.encode(data.location, provider));
            compound.putBoolean("IsLocked", data.isLocked);
        }

        @Override
        public WaystoneData decode(CompoundTag compound, HolderLookup.Provider provider) {
            return new WaystoneData(
                WaystoneHandle.Vanilla.CompoundSerializer.decode(compound.getCompound("Handle"), provider),
                compound.getString("Name"),
                WaystoneLocationData.COMPOUND_SERIALIZER.decode(compound.getCompound("Location"), provider),
                compound.getBoolean("IsLocked")
            );
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return
                compound.contains("Handle") && WaystoneHandle.Vanilla.CompoundSerializer.isContainedIn(compound.getCompound("Handle")) &&
                compound.contains("Name") &&
                compound.contains("Location") && WaystoneLocationData.COMPOUND_SERIALIZER.isContainedIn(compound.getCompound("Location")) &&
                compound.contains("IsLocked");
        }
    }

    public static final class BufferSerializer implements BufferSerializable<WaystoneData> {

        @Override
        public Class<WaystoneData> getTargetClass() {
            return WaystoneData.class;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, WaystoneData data) {
            WaystoneHandle.Vanilla.BufferSerializer.encode(buffer, data.handle);
            StringSerializer.Buffer.encode(buffer, data.name);
            WaystoneLocationData.BUFFER_SERIALIZER.encode(buffer, data.location);
            buffer.writeBoolean(data.isLocked);
        }

        @Override
        public WaystoneData decode(RegistryFriendlyByteBuf buffer) {
            return new WaystoneData(
                WaystoneHandle.Vanilla.BufferSerializer.decode(buffer),
                StringSerializer.Buffer.decode(buffer),
                WaystoneLocationData.BUFFER_SERIALIZER.decode(buffer),
                buffer.readBoolean()
            );
        }
    }

}
