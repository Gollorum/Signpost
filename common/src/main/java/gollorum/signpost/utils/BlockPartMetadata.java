package gollorum.signpost.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.function.Function;

public record BlockPartMetadata<T extends BlockPart<T>>(
    String identifier,
    Function<Integer, MapCodec<T>> codec,
    StreamCodec<RegistryFriendlyByteBuf, T> streamCodec,
    Class<T> targetClass
) {

    public static final Codec<BlockPartMetadata> CODEC = Codec.STRING.xmap(
        key -> PostTile.partsMetadata.get(key),
        BlockPartMetadata::identifier
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockPartMetadata> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
        .<RegistryFriendlyByteBuf>mapStream(it -> it)
        .map(
            key -> PostTile.partsMetadata.get(key),
            BlockPartMetadata::identifier
        );

}
