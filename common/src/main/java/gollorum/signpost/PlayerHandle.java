package gollorum.signpost;

import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

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

    public ServerPlayer asEntity() {
        return Signpost.getServerInstance().getPlayerList().getPlayer(id);
    }

    public static final CompoundSerializable<PlayerHandle> CompoundSerializer = new CompoundSerializerImpl();
    public static final BufferSerializable<PlayerHandle> BufferSerializer = new BufferSerializerImpl();

    private static final class CompoundSerializerImpl implements CompoundSerializable<PlayerHandle> {

        @Override
        public void encode(CompoundTag compound, PlayerHandle playerHandle, HolderLookup.Provider provider) {
            compound.putUUID("Id", playerHandle.id);
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return compound.contains("Id");
        }

        @Override
        public PlayerHandle decode(CompoundTag compound, HolderLookup.Provider provider) {
            return new PlayerHandle(compound.getUUID("Id"));
        }
    }

    private static final class BufferSerializerImpl implements BufferSerializable<PlayerHandle> {
        @Override
        public Class<PlayerHandle> getTargetClass() {
            return PlayerHandle.class;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, PlayerHandle playerHandle) {
            buffer.writeUUID(playerHandle.id);
        }

        @Override
        public PlayerHandle decode(RegistryFriendlyByteBuf buffer) {
            return new PlayerHandle(buffer.readUUID());
        }
    };

}
