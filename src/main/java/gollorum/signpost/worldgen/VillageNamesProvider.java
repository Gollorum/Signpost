package gollorum.signpost.worldgen;

import com.google.common.collect.Lists;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneLibrary;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface VillageNamesProvider {

	List<VillageNamesProvider> activeProviders = Lists.newArrayList(new DefaultVillageNamesProvider());

	/* villagePos might be blockPos if no village has been found. */
	static void requestFor(BlockPos blockPos, BlockPos villagePos, World world, Random random, Consumer<String> onNameFound, Runnable onNoNameFound) {
		WaystoneLibrary.getInstance().requestAllWaystoneNames(pairs -> {
			Collection<String> allTakenNames = pairs.values();
			for(VillageNamesProvider provider : activeProviders) {
				Optional<String> name = provider.getFor(blockPos, villagePos, world, n -> !allTakenNames.contains(n), random);
				if(name.isPresent()) {
					onNameFound.accept(name.get());
					return;
				}
			}
			onNoNameFound.run();
		});
	}

	Optional<String> getFor(BlockPos blockPos, BlockPos villagePos, World world, Predicate<String> validator, Random random);

}
