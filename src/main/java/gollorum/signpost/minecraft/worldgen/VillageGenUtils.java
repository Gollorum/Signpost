package gollorum.signpost.minecraft.worldgen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ConfiguredStructureTags;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;

import java.util.List;
import java.util.Optional;

public class VillageGenUtils {

    public static BlockPos getVillageLocationFor(ServerLevel level, BlockPos pieceLocation, int maxDistance) {
        List<Holder.Reference<ConfiguredStructureFeature<?, ?>>> allHolders = level.registryAccess()
            .registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY)
            .holders()
            .filter(h -> h.is(ConfiguredStructureTags.VILLAGE))
            .toList();
        return Optional.ofNullable(level.getChunkSource().getGenerator()
                .findNearestMapFeature(level, HolderSet.direct(allHolders), pieceLocation, 1, false))
            .map(Pair::getFirst)
            .filter(villagePos -> villagePos.distManhattan(pieceLocation) <= maxDistance)
            .orElse(pieceLocation);
    }
}
