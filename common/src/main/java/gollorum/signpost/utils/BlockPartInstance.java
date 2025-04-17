package gollorum.signpost.utils;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.phys.Vec3;

public record BlockPartInstance(BlockPart blockPart, Vector3 offset) {

    public record SerializedRepresentation(BlockPart.SerializedRepresentation blockPart, Vec3 offset) {
        public BlockPartInstance deserialize(HolderLookup.Provider registryAccess) {
            return new BlockPartInstance(blockPart.deserialize(registryAccess), Vector3.fromVec3d(offset));
        }
    }

    public static final Codec<SerializedRepresentation> CODEC = Codec.pair(
        BlockPart.CODEC,
        Vec3.CODEC
    ).xmap(
        pair -> new SerializedRepresentation(pair.getFirst(), pair.getSecond()),
        instance -> Pair.of(instance.blockPart, instance.offset)
    );

}
