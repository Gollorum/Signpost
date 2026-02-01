package gollorum.signpost.utils.serialization;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import gollorum.signpost.utils.Either;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public class WorldSerializer {

    public static final MapCodec<Either<Level, Identifier>> MAP_CODEC = Codec.STRING
        .fieldOf("DimensionId")
        .xmap(
            loc -> Either.right(Identifier.parse(loc)),
            either -> either.rightOr(level -> level.dimension().identifier()).toString()
        );

    public static final StreamCodec<ByteBuf, Either<Level, Identifier>> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
        .map(
            loc -> Either.right(Identifier.parse(loc)),
            either -> either.rightOr(level -> level.dimension().identifier()).toString()
        );

}
