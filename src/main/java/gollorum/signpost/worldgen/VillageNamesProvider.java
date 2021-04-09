package gollorum.signpost.worldgen;

import com.google.common.collect.Lists;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneLibrary;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface VillageNamesProvider {

	List<VillageNamesProvider> activeProviders = Lists.newArrayList(new DefaultVillageNamesProvider());

	/* villagePos might be blockPos if no village has been found. */
	static Optional<String> requestFor(BlockPos blockPos, BlockPos villagePos, ServerWorld world, Random random) {
		Set<String> allTakenNames = WaystoneLibrary.getInstance().getAllWaystoneNames().get();
		for(VillageNamesProvider provider : activeProviders) {
			Optional<String> name = provider.getFor(blockPos, villagePos, world, n -> !allTakenNames.contains(n), random);
			if(name.isPresent()) {
				return name;
			}
		}
		return Optional.empty();
	}

	Optional<String> getFor(BlockPos blockPos, BlockPos villagePos, ServerWorld world, Predicate<String> validator, Random random);

}
