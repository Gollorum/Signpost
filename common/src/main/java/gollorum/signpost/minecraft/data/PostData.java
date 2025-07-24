package gollorum.signpost.minecraft.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import gollorum.signpost.utils.BlockPartInstance;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record PostData(Map<UUID, BlockPartInstance> parts) {

    public static final Codec<PostData> CODEC = Codec.pair(
        UUIDUtil.CODEC,
        BlockPartInstance.CODEC
    ).listOf().xmap(
        list -> new PostData(list.stream().collect(Collectors.toMap(
            Pair::getFirst,
            Pair::getSecond
        ))),
        data -> data.parts().entrySet().stream()
            .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList())
    );

    public static final DataComponentType<PostData> TYPE =
        new DataComponentType.Builder<PostData>().persistent(PostData.CODEC).build();

}