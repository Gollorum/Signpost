package gollorum.signpost.worldgen;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;

import java.util.Optional;

public class WorldGenUtils {

	public static Optional<BlockPos> findNearestVillage(ServerWorld world, BlockPos pos, int radius) {
		return Optional.ofNullable(world.func_241117_a_(Structure.VILLAGE.getStructure(), pos, radius, false));
	}

}
