package gollorum.signpost.minecraft.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.utils.serialization.OptionalKeyDispatchCodec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record PostData(Map<UUID, BlockPartInstance> parts) {

    public static final Codec<PostData> CODEC_V1 = Codec.dispatchedMap(
        BlockPartMetadata.CODEC,
        key -> Codec.list(BlockPartInstance.codecV1(key))
    ).xmap(
        map -> {
            var newMap = new java.util.HashMap<UUID, BlockPartInstance>();
            for (var entry : map.entrySet()) {
                for (var pair : entry.getValue()) {
                    newMap.put(pair.getSecond().orElse(UUID.randomUUID()), pair.getFirst());
                }
            }
            return new PostData(newMap);
        },
        postData -> {
            var newMap = new java.util.HashMap<BlockPartMetadata, List<Pair<BlockPartInstance, Optional<UUID>>>>();
            for (var entry : postData.parts.entrySet()) {
                var meta = entry.getValue().blockPart().getMeta();
                newMap.computeIfAbsent(meta, k -> new java.util.ArrayList<>())
                    .add(Pair.of(entry.getValue(), Optional.of(entry.getKey())));
            }
            return newMap;
        }
    );

    public static final Codec<PostData> CODEC_V2 = Codec.unboundedMap(
        UUIDUtil.STRING_CODEC,
        BlockPartInstance.CODEC_V2
    ).xmap(PostData::new, PostData::parts);

    public static final MapCodec<PostData> CODEC = new OptionalKeyDispatchCodec<>(
        "DataVersion", 1, data -> 2,
        Codec.INT,
        version -> (version == 1 ? CODEC_V1 : CODEC_V2).fieldOf("Parts")
    );

    public static final DataComponentType<PostData> TYPE =
        new DataComponentType.Builder<PostData>().persistent(PostData.CODEC.codec()).build();

}