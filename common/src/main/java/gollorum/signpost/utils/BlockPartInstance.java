package gollorum.signpost.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record BlockPartInstance(BlockPart blockPart, Vector3 offset) {

    public static final MapCodec<BlockPartInstance> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        BlockPart.CODEC.forGetter(BlockPartInstance::blockPart),
        Vector3.CODEC.fieldOf("Offset").forGetter(BlockPartInstance::offset)
    ).apply(i, BlockPartInstance::new));

    public static final Codec<BlockPartInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
        BlockPart.CODEC.forGetter(BlockPartInstance::blockPart),
        Vector3.CODEC.fieldOf("Offset").forGetter(BlockPartInstance::offset)
    ).apply(i, BlockPartInstance::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockPartInstance> STREAM_CODEC = StreamCodec.composite(
        BlockPart.STREAM_CODEC, BlockPartInstance::blockPart,
        Vector3.STREAM_CODEC, BlockPartInstance::offset,
        BlockPartInstance::new
    );

}
