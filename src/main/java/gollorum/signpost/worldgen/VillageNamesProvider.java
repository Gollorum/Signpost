package gollorum.signpost.worldgen;

import com.google.common.collect.Lists;
import gollorum.signpost.WaystoneLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public interface VillageNamesProvider {

	List<VillageNamesProvider> activeProviders = Lists.newArrayList(new DefaultVillageNamesProvider());

	/* villagePos might be blockPos if no village has been found. */
	static Optional<String> requestFor(BlockPos blockPos, BlockPos villagePos, ServerLevel world, Random random) {
		Set<String> allTakenNames = WaystoneLibrary.getInstance().getAllWaystoneNames().get();
		for(VillageNamesProvider provider : activeProviders) {
			Optional<String> name = provider.getFor(blockPos, villagePos, world, n -> !allTakenNames.contains(n), random);
			if(name.isPresent()) {
				return name;
			}
		}
		return Optional.empty();
	}

	Optional<String> getFor(BlockPos blockPos, BlockPos villagePos, ServerLevel world, Predicate<String> validator, Random random);

}
