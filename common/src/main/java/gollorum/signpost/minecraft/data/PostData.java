package gollorum.signpost.minecraft.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.utils.serialization.OptionalKeyDispatchCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceKey;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record PostData(ResourceKey<PostBlock.ModelType> modelType, Map<UUID, BlockPartInstance> parts) {

    public static final MapCodec<PostData> CODEC_V1 = Codec.dispatchedMap(
        BlockPartMetadata.CODEC,
        key -> Codec.list(BlockPartInstance.codecV1(key))
    ).fieldOf("Parts").xmap(
        map -> {
            var newMap = new java.util.HashMap<UUID, BlockPartInstance>();
            for (var entry : map.entrySet()) {
                for (var pair : entry.getValue()) {
                    newMap.put(pair.getSecond().orElse(UUID.randomUUID()), pair.getFirst());
                }
            }
            return new PostData(null, newMap); // TODO PREMERGE
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

    private static final Codec<Map<UUID, BlockPartInstance>> PARTS_MAP_CODEC = Codec.unboundedMap(
        UUIDUtil.STRING_CODEC,
        BlockPartInstance.CODEC_V2
    );

    public static final MapCodec<PostData> CODEC_V2 = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                ResourceKey.codec(ModelTypeRegistry.REGISTRY_KEY).fieldOf("ModelType").forGetter(PostData::modelType),
                PARTS_MAP_CODEC.fieldOf("Parts").forGetter(PostData::parts)
            ).apply(instance, PostData::new)
        );

    public static final MapCodec<PostData> CODEC = new OptionalKeyDispatchCodec<>(
            "DataVersion", 1, data -> 2,
            Codec.INT,
            version -> (version == 1 ? CODEC_V1 : CODEC_V2)
        );

    public static final DataComponentType<PostData> TYPE =
        new DataComponentType.Builder<PostData>().persistent(PostData.CODEC.codec()).build();

}