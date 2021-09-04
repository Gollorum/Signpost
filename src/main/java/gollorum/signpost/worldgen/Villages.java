package gollorum.signpost.worldgen;

import com.google.common.collect.ImmutableList;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.worldgen.SignpostJigsawPiece;
import gollorum.signpost.minecraft.worldgen.WaystoneJigsawPiece;
import gollorum.signpost.utils.CollectionUtils;
import gollorum.signpost.utils.Tuple;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.data.worldgen.VillagePools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.structures.SinglePoolElement;
import net.minecraft.world.level.levelgen.feature.structures.StructurePoolElement;
import net.minecraft.world.level.levelgen.feature.structures.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Villages {

	public static final Villages instance = new Villages();
	private Villages() { VillagePools.bootstrap(); }

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

		public ResourceLocation getStructureResourceLocation(String structureName) {
			return new ResourceLocation(Signpost.MOD_ID, "village/" + name + "/" + structureName);
		}
	}

	private StructureProcessorList waystoneProcessorListDesert;
	private StructureProcessorList waystoneProcessorListPlains;
	private StructureProcessorList waystoneProcessorListSavanna;
	private StructureProcessorList waystoneProcessorListSnowyOrTaiga;

	private void registerProcessorLists() {
		waystoneProcessorListDesert = ProcessorLists.EMPTY;
		waystoneProcessorListPlains = ProcessorLists.STREET_PLAINS;
		waystoneProcessorListSavanna = ProcessorLists.STREET_SAVANNA;
		waystoneProcessorListSnowyOrTaiga = ProcessorLists.STREET_SNOWY_OR_TAIGA;
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
					new WaystoneJigsawPiece(villageType.getStructureResourceLocation("waystone"),
						villageType.processorList, StructureTemplatePool.Projection.RIGID),
					5
				),
				Tuple.of(
					new SignpostJigsawPiece(villageType.getStructureResourceLocation("signpost"),
						villageType.processorList, StructureTemplatePool.Projection.TERRAIN_MATCHING, isZombie),
					5
				)
			),
			isZombie ? getZombieVillagePool(villageType) : getVillagePool(villageType)
		);
	}

	private static ResourceLocation getVillagePool(VillageType villageType) {
		return  new ResourceLocation("village/" + villageType.name + "/houses");
	}

	private static ResourceLocation getZombieVillagePool(VillageType villageType) {
		return  new ResourceLocation("village/" + villageType.name + "/zombie/houses");
	}

	private void addToPool(
		Collection<Tuple<SinglePoolElement, Integer>> houses, ResourceLocation pool
	) {
		StructureTemplatePool oldPattern = BuiltinRegistries.TEMPLATE_POOL.get(pool);
		if(oldPattern == null) {
			Signpost.LOGGER.error("Tried to add elements to village pool " + pool + ", but it was not found in the registry.");
			return;
		}
		Map<StructurePoolElement, Integer> allPieces = CollectionUtils.group(oldPattern.templates);
		for(Tuple<SinglePoolElement, Integer> tuple : houses) {
			allPieces.put(tuple._1, tuple._2);
		}
		registerPoolEntries(allPieces, pool, oldPattern.getName());
	}

	private static void registerPoolEntries(Map<StructurePoolElement, Integer> pieces, ResourceLocation pool, ResourceLocation patternName) {
		Registry.register(BuiltinRegistries.TEMPLATE_POOL, pool, new StructureTemplatePool(pool, patternName, pieces.entrySet().stream().map(e -> com.mojang.datafixers.util.Pair
			.of(e.getKey(), e.getValue())).collect(Collectors.toList())));
	}

}
