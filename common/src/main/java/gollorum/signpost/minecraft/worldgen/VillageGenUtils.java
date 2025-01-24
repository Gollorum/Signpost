package gollorum.signpost.minecraft.worldgen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;

import java.util.Optional;

public class VillageGenUtils {

    public static BlockPos getVillageLocationFor(ServerLevel level, BlockPos pieceLocation, int maxDistance) {
        return level.registryAccess()
            .lookup(Registries.STRUCTURE)
            .flatMap(reg -> reg.get(StructureTags.VILLAGE))
            .flatMap(holders -> Optional.ofNullable(level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, holders, pieceLocation, 100, false)))
            .map(Pair::getFirst)
            .filter(villagePos -> villagePos.distManhattan(pieceLocation) <= maxDistance)
            .orElse(pieceLocation);
    }
}
