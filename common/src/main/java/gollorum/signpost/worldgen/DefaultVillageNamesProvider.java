package gollorum.signpost.worldgen;

import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.apache.commons.lang3.text.WordUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DefaultVillageNamesProvider implements VillageNamesProvider {


	@Override
	public Optional<String> getFor(
		BlockPos blockPos, BlockPos villagePos, ServerLevel world, Predicate<String> validator, Random random
	) {
        var namingConfig = IConfig.IServer.getInstance().worldGen().naming();
		List<? extends String> prefixes = namingConfig.villageNamePrefixes();
		List<? extends String> infixes = namingConfig.villageNameInfixes();
		List<? extends String> postfixes = namingConfig.villageNamePostfixes();
		List<Integer> prefixIndices = IntStream.range(0, prefixes.size()).boxed().collect(Collectors.toList());
		List<Integer> infixIndices = IntStream.range(0, infixes.size()).boxed().collect(Collectors.toList());
		List<Integer> postfixIndices = IntStream.range(0, postfixes.size()).boxed().collect(Collectors.toList());
		Collections.shuffle(prefixIndices, random);
		Collections.shuffle(infixIndices, random);
		Collections.shuffle(postfixIndices, random);
		for(int i : prefixIndices)
			for(int j : infixIndices)
				for(int k : postfixIndices) {
					String name = WordUtils.capitalize((String) prefixes.get(i) + infixes.get(j) + postfixes.get(k)).trim();
					if(validator.test(name)) return Optional.of(name);
				}
		return Optional.empty();
	}

}
