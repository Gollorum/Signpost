package gollorum.signpost.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.security.WithOwner;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public record WaystoneData(WaystoneHandle.Vanilla handle, String name, WaystoneLocationData location, boolean isLocked) implements gollorum.signpost.WaystoneDataBase {

    public WaystoneData withoutExplicitLevel() {
        if(location.block().world().isLeft()) {
            return new WaystoneData(
                handle,
                name,
                new WaystoneLocationData(
                    WorldLocation.from(location.block().blockPos(), Either.right(location.block().world().leftOrThrow().dimension().identifier())),
                    location.spawn()),
                isLocked);
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
        return player.permissions().hasPermission(IConfig.IServer.getInstance().permissions().editLockedWaystoneCommandPermission())
            || TileEntityUtils.toWorld(locationData.block().world(), !(player instanceof ServerPlayer))
                .map(w -> w.getBlockEntity(locationData.block().blockPos()))
                .flatMap(tile -> tile instanceof WithOwner.OfWaystone ? ((WithOwner.OfWaystone)tile).getWaystoneOwner() : Optional.empty())
                .map(owner -> owner.id().equals(player.getUUID()))
                .orElse(true);
    }

    @Override
    public WaystoneLocationData loc() {
        return location;
    }

    public static final Codec<WaystoneData> CODEC = RecordCodecBuilder.create(i -> i.group(
        WaystoneHandle.Vanilla.CODEC.fieldOf("handle").forGetter(WaystoneData::handle),
        Codec.STRING.fieldOf("Name").forGetter(WaystoneData::name),
        WaystoneLocationData.CODEC.fieldOf("Location").forGetter(WaystoneData::location),
        Codec.BOOL.fieldOf("IsLocked").forGetter(WaystoneData::isLocked)
    ).apply(i, WaystoneData::new));

    public static final StreamCodec<ByteBuf, WaystoneData> STREAM_CODEC = StreamCodec.composite(
        WaystoneHandle.Vanilla.STREAM_CODEC, WaystoneData::handle,
        ByteBufCodecs.STRING_UTF8, WaystoneData::name,
        WaystoneLocationData.STREAM_CODEC, WaystoneData::location,
        ByteBufCodecs.BOOL, WaystoneData::isLocked,
        WaystoneData::new
    );

}
