package gollorum.signpost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;
import java.util.UUID;

public record PlayerHandle(UUID id) {

	public static final PlayerHandle Invalid = new PlayerHandle((LivingEntity) null);

    public PlayerHandle(Entity player) {
        this(player == null ? Util.NIL_UUID : player.getUUID());
    }

    public static PlayerHandle from(Entity player) {
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
