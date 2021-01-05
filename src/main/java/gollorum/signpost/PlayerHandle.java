package gollorum.signpost;

import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public class PlayerHandle {
    public final UUID id;

    public PlayerHandle(@Nonnull UUID id) {
        this.id = id;
    }

    public PlayerHandle(@Nullable LivingEntity player) {
        this.id = player == null ? UUID.randomUUID() : player.getUniqueID();
    }

    public static PlayerHandle from(@Nullable LivingEntity player) {
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

    public static final Serializer SERIALIZER  = new Serializer();

    public static class Serializer implements CompoundSerializable<PlayerHandle> {

        @Override
        public void writeTo(PlayerHandle playerHandle, CompoundNBT compound, String keyPrefix) {
            compound.putUniqueId(keyPrefix + "Id", playerHandle.id);
        }

        @Override
        public boolean isContainedIn(CompoundNBT compound, String keyPrefix) {
            return compound.contains(keyPrefix + "Id");
        }

        @Override
        public PlayerHandle read(CompoundNBT compound, String keyPrefix) {
            return new PlayerHandle(compound.getUniqueId(keyPrefix + "Id"));
        }

        @Override
        public void writeTo(PlayerHandle playerHandle, PacketBuffer buffer) {
            buffer.writeUniqueId(playerHandle.id);
        }

        @Override
        public PlayerHandle readFrom(PacketBuffer buffer) {
            return new PlayerHandle(buffer.readUniqueId());
        }
    }

}
