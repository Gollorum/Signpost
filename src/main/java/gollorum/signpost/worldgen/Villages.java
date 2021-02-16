package gollorum.signpost.worldgen;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import gollorum.signpost.Signpost;
import gollorum.signpost.utils.CollectionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.gen.feature.jigsaw.JigsawPattern;
import net.minecraft.world.gen.feature.jigsaw.JigsawPiece;
import net.minecraft.world.gen.feature.jigsaw.LegacySingleJigsawPiece;
import net.minecraft.world.gen.feature.structure.VillagesPools;
import net.minecraft.world.gen.feature.template.ProcessorLists;
import net.minecraft.world.gen.feature.template.StructureProcessorList;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Villages {

	public static final Villages instance = new Villages();
	private Villages() { VillagesPools.func_244194_a(); }

	private enum VillageType {
		Desert("desert", () -> instance.waystoneProcessorListDesert),
		Plains("plains", () -> instance.waystoneProcessorListPlains),
		Savanna("savanna", () -> instance.waystoneProcessorListSavanna),
		Snowy("snowy", () -> instance.waystoneProcessorListSnowyOrTaiga),
		Taiga("taiga", () -> instance.waystoneProcessorListSnowyOrTaiga);
		public final String name;
		public final Supplier<StructureProcessorList> processorList;

		VillageType(String name, Supplier<StructureProcessorList> processorList) {
			this.name = name;
			this.processorList = processorList;
		}
	}

	private StructureProcessorList waystoneProcessorListDesert;
	private StructureProcessorList waystoneProcessorListPlains;
	private StructureProcessorList waystoneProcessorListSavanna;
	private StructureProcessorList waystoneProcessorListSnowyOrTaiga;

	private void registerProcessorLists() {
		waystoneProcessorListDesert = ProcessorLists.field_244101_a;
		waystoneProcessorListPlains = ProcessorLists.field_244110_j;
		waystoneProcessorListSavanna = ProcessorLists.field_244111_k;
		waystoneProcessorListSnowyOrTaiga = ProcessorLists.field_244112_l;
	}

	public void registerWaystones() {
		registerProcessorLists();
		for(VillageType villageType : VillageType.values()) {
			ResourceLocation pool = new ResourceLocation("village/" + villageType.name + "/houses");
			addToPool(
				ImmutableList.of(
					new ResourceLocation(Signpost.MOD_ID, "village/" + villageType.name + "/waystone"),
					new ResourceLocation(Signpost.MOD_ID, "village/" + villageType.name + "/signpost")
				),
				5,
				JigsawPattern.PlacementBehaviour.TERRAIN_MATCHING,
				villageType.processorList.get(),
				pool
			);
		}
	}

	private void addToPool(
		Collection<ResourceLocation> houses, int weight, JigsawPattern.PlacementBehaviour placementBehaviour, StructureProcessorList processorList, ResourceLocation pool
	) {
		JigsawPattern oldPattern = WorldGenRegistries.JIGSAW_POOL.getOrDefault(pool);
		if(oldPattern == null) {
			Signpost.LOGGER.error("Tried to add elements to village pool " + pool + ", but it was not found in the registry.");
			return;
		}
		Map<JigsawPiece, Integer> allPieces = CollectionUtils.group(oldPattern.getShuffledPieces(new Random(0)));
		for(ResourceLocation loc : houses) {
			allPieces.put(
				LegacySingleJigsawPiece.func_242851_a(loc.toString(), processorList)
				.apply(placementBehaviour),
				weight
			);
		}
		Registry.register(WorldGenRegistries.JIGSAW_POOL, pool, new JigsawPattern(pool, oldPattern.getName(), allPieces.entrySet().stream().map(e -> Pair
			.of(e.getKey(), e.getValue())).collect(Collectors.toList())));
	}

}
