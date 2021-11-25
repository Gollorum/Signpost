package gollorum.signpost.worldgen;

import com.google.common.collect.ImmutableList;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.worldgen.SignpostJigsawPiece;
import gollorum.signpost.minecraft.worldgen.WaystoneJigsawPiece;
import gollorum.signpost.utils.CollectionUtils;
import gollorum.signpost.utils.Tuple;
import net.minecraft.block.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.gen.feature.jigsaw.JigsawManager;
import net.minecraft.world.gen.feature.jigsaw.JigsawPattern;
import net.minecraft.world.gen.feature.jigsaw.JigsawPiece;
import net.minecraft.world.gen.feature.jigsaw.SingleJigsawPiece;
import net.minecraft.world.gen.feature.structure.*;
import net.minecraft.world.gen.feature.template.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Villages {

	public static final Villages instance = new Villages();
	private Villages() {
		PlainsVillagePools.bootstrap();
		SnowyVillagePools.bootstrap();
		SavannaVillagePools.bootstrap();
		DesertVillagePools.bootstrap();
		TaigaVillagePools.bootstrap();
	}

	private enum VillageType {
		Desert("desert", instance.waystoneProcessorListDesert),
		Plains("plains", instance.waystoneProcessorListPlains),
		Savanna("savanna", instance.waystoneProcessorListSavanna),
		Snowy("snowy", instance.waystoneProcessorListSnowyOrTaiga),
		Taiga("taiga", instance.waystoneProcessorListSnowyOrTaiga);
		public final String name;
		public final List<StructureProcessor> processorList;

		VillageType(String name, List<StructureProcessor> processorList) {
			this.name = name;
			this.processorList = processorList;
		}

		public ResourceLocation getStructureResourceLocation(String structureName) {
			return new ResourceLocation(Signpost.MOD_ID, "village/" + name + "/" + structureName);
		}
	}

	private List<StructureProcessor> waystoneProcessorListDesert;
	private List<StructureProcessor> waystoneProcessorListPlains;
	private List<StructureProcessor> waystoneProcessorListSavanna;
	private List<StructureProcessor> waystoneProcessorListSnowyOrTaiga;

	private void registerProcessorLists() {
		waystoneProcessorListDesert = ImmutableList.of();
		waystoneProcessorListPlains = ImmutableList.of(new RuleStructureProcessor(ImmutableList.of(new RuleEntry(new BlockMatchRuleTest(Blocks.GRASS_PATH), new BlockMatchRuleTest(Blocks.WATER), Blocks.OAK_PLANKS.defaultBlockState()), new RuleEntry(new RandomBlockMatchRuleTest(Blocks.GRASS_PATH, 0.1F), AlwaysTrueRuleTest.INSTANCE, Blocks.GRASS_BLOCK.defaultBlockState()), new RuleEntry(new BlockMatchRuleTest(Blocks.GRASS_BLOCK), new BlockMatchRuleTest(Blocks.WATER), Blocks.WATER.defaultBlockState()), new RuleEntry(new BlockMatchRuleTest(Blocks.DIRT), new BlockMatchRuleTest(Blocks.WATER), Blocks.WATER.defaultBlockState()))));
		waystoneProcessorListSavanna = ImmutableList.of(new RuleStructureProcessor(ImmutableList.of(new RuleEntry(new BlockMatchRuleTest(Blocks.GRASS_PATH), new BlockMatchRuleTest(Blocks.WATER), Blocks.ACACIA_PLANKS.defaultBlockState()), new RuleEntry(new RandomBlockMatchRuleTest(Blocks.GRASS_PATH, 0.2F), AlwaysTrueRuleTest.INSTANCE, Blocks.GRASS_BLOCK.defaultBlockState()), new RuleEntry(new BlockMatchRuleTest(Blocks.GRASS_BLOCK), new BlockMatchRuleTest(Blocks.WATER), Blocks.WATER.defaultBlockState()), new RuleEntry(new BlockMatchRuleTest(Blocks.DIRT), new BlockMatchRuleTest(Blocks.WATER), Blocks.WATER.defaultBlockState()))));
		waystoneProcessorListSnowyOrTaiga = ImmutableList.of(new RuleStructureProcessor(ImmutableList.of(new RuleEntry(new BlockMatchRuleTest(Blocks.GRASS_PATH), new BlockMatchRuleTest(Blocks.WATER), Blocks.SPRUCE_PLANKS.defaultBlockState()), new RuleEntry(new RandomBlockMatchRuleTest(Blocks.GRASS_PATH, 0.2F), AlwaysTrueRuleTest.INSTANCE, Blocks.GRASS_BLOCK.defaultBlockState()), new RuleEntry(new BlockMatchRuleTest(Blocks.GRASS_BLOCK), new BlockMatchRuleTest(Blocks.WATER), Blocks.WATER.defaultBlockState()), new RuleEntry(new BlockMatchRuleTest(Blocks.DIRT), new BlockMatchRuleTest(Blocks.WATER), Blocks.WATER.defaultBlockState()))));
	}

	public void reset() {
		WaystoneJigsawPiece.reset();
		SignpostJigsawPiece.reset();
	}

	public void initialize() {
		registerProcessorLists();
		for(VillageType villageType : VillageType.values()) {
			registerFor(villageType, true);
			registerFor(villageType, false);
		}
	}

	private void registerFor(VillageType villageType, boolean isZombie) {
		addToPool(
			ImmutableList.of(
				Tuple.of(
					new WaystoneJigsawPiece(villageType.getStructureResourceLocation("waystone").toString(),
						villageType.processorList, JigsawPattern.PlacementBehaviour.RIGID),
					1
				),
				Tuple.of(
					new SignpostJigsawPiece(villageType.getStructureResourceLocation("signpost").toString(),
						villageType.processorList, JigsawPattern.PlacementBehaviour.TERRAIN_MATCHING, isZombie),
					3
				)
			),
			isZombie ? getZombieVillagePool(villageType) : getVillagePool(villageType),
			JigsawPattern.PlacementBehaviour.RIGID
		);
	}

	private static ResourceLocation getVillagePool(VillageType villageType) {
		return  new ResourceLocation("village/" + villageType.name + "/houses");
	}

	private static ResourceLocation getZombieVillagePool(VillageType villageType) {
		return  new ResourceLocation("village/" + villageType.name + "/zombie/houses");
	}

	private void addToPool(
		Collection<Tuple<SingleJigsawPiece, Integer>> houses, ResourceLocation pool, JigsawPattern.PlacementBehaviour placementBehaviour
	) {
		JigsawPattern oldPattern = JigsawManager.POOLS.getPool(pool);
		if(oldPattern == JigsawPattern.INVALID) {
			Signpost.LOGGER.error("Tried to add elements to village pool " + pool + ", but it was not found in the registry.");
			return;
		}
		Map<JigsawPiece, Integer> allPieces = CollectionUtils.group(oldPattern.templates);
		for(Tuple<SingleJigsawPiece, Integer> tuple : houses) {
			allPieces.put(tuple._1, tuple._2);
		}
		registerPoolEntries(allPieces, oldPattern.getName(), oldPattern.getFallback(), placementBehaviour);
	}

	private static void registerPoolEntries(Map<JigsawPiece, Integer> pieces, ResourceLocation pool, ResourceLocation patternName, JigsawPattern.PlacementBehaviour placementBehaviour) {
		JigsawManager.POOLS.register(new JigsawPattern(pool, patternName, pieces.entrySet().stream().map(e -> com.mojang.datafixers.util.Pair
			.of(e.getKey(), e.getValue())).collect(Collectors.toList()), placementBehaviour));
	}

}
