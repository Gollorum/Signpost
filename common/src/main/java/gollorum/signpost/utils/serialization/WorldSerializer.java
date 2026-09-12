package gollorum.signpost.utils.serialization;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import gollorum.signpost.utils.Either;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class WorldSerializer {

    public static final MapCodec<Either<Level, ResourceLocation>> MAP_CODEC = Codec.STRING
        .fieldOf("DimensionId")
        .xmap(
            loc -> Either.right(ResourceLocation.parse(loc)),
            either -> either.rightOr(level -> level.dimension().location()).toString()
        );

    public static final StreamCodec<ByteBuf, Either<Level, ResourceLocation>> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
        .map(
            loc -> Either.right(ResourceLocation.parse(loc)),
            either -> either.rightOr(level -> level.dimension().location()).toString()
        );

}
