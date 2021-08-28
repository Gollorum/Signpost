package gollorum.signpost;

import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public class PlayerHandle {

	public static final PlayerHandle Invalid = new PlayerHandle((LivingEntity) null);
	public final UUID id;

    public PlayerHandle(@Nonnull UUID id) {
        this.id = id;
    }

    public PlayerHandle(@Nullable Entity player) {
        this.id = player == null ? Util.NIL_UUID : player.getUUID();
    }

    public static PlayerHandle from(@Nullable Entity player) {
        return new PlayerHandle(player);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerHandle that = (PlayerHandle) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public PlayerEntity asEntity() {
        return Signpost.getServerInstance().getPlayerList().getPlayer(id);
    }

    public static final CompoundSerializable<PlayerHandle> Serializer = new SerializerImpl();
    public static final class SerializerImpl implements CompoundSerializable<PlayerHandle> {

        @Override
        public CompoundNBT write(PlayerHandle playerHandle, CompoundNBT compound) {
            compound.putUUID("Id", playerHandle.id);
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundNBT compound) {
            return compound.contains("Id");
        }

        @Override
        public PlayerHandle read(CompoundNBT compound) {
            return new PlayerHandle(compound.getUUID("Id"));
        }

        @Override
        public Class<PlayerHandle> getTargetClass() {
            return PlayerHandle.class;
        }

        @Override
        public void write(PlayerHandle playerHandle, PacketBuffer buffer) {
            buffer.writeUUID(playerHandle.id);
        }

        @Override
        public PlayerHandle read(PacketBuffer buffer) {
            return new PlayerHandle(buffer.readUUID());
        }
    };

}
