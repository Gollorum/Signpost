package gollorum.signpost.minecraft.worldgen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

public class VillageGenUtils {

    public static BlockPos getVillageLocationFor(ServerLevel level, BlockPos pieceLocation, int maxDistance) {
        var allHolders = level.registryAccess()
            .registryOrThrow(Registries.STRUCTURE)
            .holders()
            .filter(h -> h.is(StructureTags.VILLAGE))
            .toList();
        return Optional.ofNullable(level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(allHolders), pieceLocation, 100, false))
            .map(Pair::getFirst)
            .filter(villagePos -> villagePos.distManhattan(pieceLocation) <= maxDistance)
            .orElse(pieceLocation);
    }
}
