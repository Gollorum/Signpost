package gollorum.signpost;

import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.OptionalSerializer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class PlayerHandle {

	public static final PlayerHandle Invalid = new PlayerHandle((LivingEntity) null);
	public final UUID id;

    public PlayerHandle(@Nonnull UUID id) {
        this.id = id;
    }

    public PlayerHandle(@Nullable Entity player) {
        this.id = player == null ? Util.DUMMY_UUID : player.getUniqueID();
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
        return Signpost.getServerInstance().getPlayerList().getPlayerByUUID(id);
    }

    public static final CompoundSerializable<PlayerHandle> Serializer = new CompoundSerializable<PlayerHandle>() {

        @Override
        public CompoundNBT write(PlayerHandle playerHandle, CompoundNBT compound) {
            compound.putUniqueId("Id", playerHandle.id);
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundNBT compound) {
            return compound.contains("Id");
        }

        @Override
        public PlayerHandle read(CompoundNBT compound) {
            return new PlayerHandle(compound.getUniqueId("Id"));
        }

        @Override
        public void write(PlayerHandle playerHandle, PacketBuffer buffer) {
            buffer.writeUniqueId(playerHandle.id);
        }

        @Override
        public PlayerHandle read(PacketBuffer buffer) {
            return new PlayerHandle(buffer.readUniqueId());
        }
    };

}
