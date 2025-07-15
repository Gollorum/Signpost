package gollorum.signpost.minecraft.data;

import com.mojang.serialization.Codec;
import gollorum.signpost.utils.BlockPartInstance;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;

import java.util.Map;
import java.util.UUID;

public record PostData(Map<UUID, BlockPartInstance> parts) {

    public static final Codec<PostData> CODEC = Codec.unboundedMap(
        UUIDUtil.CODEC,
        BlockPartInstance.CODEC
    ).xmap(PostData::new, PostData::parts);

    public static final DataComponentType<PostData> TYPE =
        new DataComponentType.Builder<PostData>().persistent(PostData.CODEC).build();

}