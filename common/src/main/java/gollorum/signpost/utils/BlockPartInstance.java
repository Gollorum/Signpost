package gollorum.signpost.utils;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

public record BlockPartInstance(BlockPart blockPart, Vector3 offset) {

    public static Codec<Pair<BlockPartInstance, Optional<UUID>>> codecV1(BlockPartMetadata meta) {
        return RecordCodecBuilder.create(i -> i.group(
            ((MapCodec<BlockPart>) meta.codec().apply(1)).forGetter(pair -> pair.getFirst().blockPart()),
            Vector3.CODEC.fieldOf("Offset").forGetter(pair -> pair.getFirst().offset()),
            UUIDUtil.CODEC.optionalFieldOf("PartId").forGetter(Pair::getSecond)
        ).apply(i, (part, offset, id) -> Pair.of(new BlockPartInstance(part, offset), id)));
    }

    public static final Codec<BlockPartInstance> CODEC_V2 = RecordCodecBuilder.create(i -> i.group(
        BlockPart.codec(2).forGetter(BlockPartInstance::blockPart),
        Vector3.CODEC.fieldOf("Offset").forGetter(BlockPartInstance::offset)
    ).apply(i, BlockPartInstance::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockPartInstance> STREAM_CODEC = StreamCodec.composite(
        BlockPart.STREAM_CODEC, BlockPartInstance::blockPart,
        Vector3.STREAM_CODEC, BlockPartInstance::offset,
        BlockPartInstance::new
    );

}
