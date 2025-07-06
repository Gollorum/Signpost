package gollorum.signpost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import io.netty.buffer.ByteBuf;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public record PlayerHandle(@Nonnull UUID id) {

	public static final PlayerHandle Invalid = new PlayerHandle((LivingEntity) null);

    public PlayerHandle(@Nullable Entity player) {
        this(player == null ? Util.NIL_UUID : player.getUUID());
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

    public static final Codec<PlayerHandle> CODEC = RecordCodecBuilder.create(i -> i.group(
        UUIDUtil.CODEC.fieldOf("Id").forGetter(PlayerHandle::id)
    ).apply(i, PlayerHandle::new));

    public static final StreamCodec<ByteBuf, PlayerHandle> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        PlayerHandle::id,
        PlayerHandle::new
    );

}
