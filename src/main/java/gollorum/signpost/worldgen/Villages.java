package gollorum.signpost.worldgen;

import com.google.common.collect.ImmutableList;
import gollorum.signpost.Signpost;
import gollorum.signpost.utils.CollectionUtils;
import gollorum.signpost.utils.Tuple;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.data.worldgen.VillagePools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class Villages {

	public static final Villages instance = new Villages();
	private Villages() { VillagePools.bootstrap(); }

	private enum VillageType {
		Desert("desert", instance.waystoneProcessorListDesert),
		Plains("plains", instance.waystoneProcessorListPlains),
		Savanna("savanna", instance.waystoneProcessorListSavanna),
		Snowy("snowy", instance.waystoneProcessorListSnowyOrTaiga),
		Taiga("taiga", instance.waystoneProcessorListSnowyOrTaiga);
		public final String name;
		public final Holder<StructureProcessorList> processorList;

		VillageType(String name, Holder<StructureProcessorList> processorList) {
			this.name = name;
			this.processorList = processorList;
		}

		public ResourceLocation getStructureResourceLocation(String structureName) {
			return new ResourceLocation(Signpost.MOD_ID, "village/" + name + "/" + structureName);
		}
	}

	private Holder<StructureProcessorList> waystoneProcessorListDesert;
	private Holder<StructureProcessorList> waystoneProcessorListPlains;
	private Holder<StructureProcessorList> waystoneProcessorListSavanna;
	private Holder<StructureProcessorList> waystoneProcessorListSnowyOrTaiga;

	private void registerProcessorLists() {
		waystoneProcessorListDesert = ProcessorLists.EMPTY;
		waystoneProcessorListPlains = ProcessorLists.STREET_PLAINS;
		waystoneProcessorListSavanna = ProcessorLists.STREET_SAVANNA;
		waystoneProcessorListSnowyOrTaiga = ProcessorLists.STREET_SNOWY_OR_TAIGA;
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
					StructurePoolElement.single(
						villageType.getStructureResourceLocation("waystone").toString(),
					    villageType.processorList
					).apply(StructureTemplatePool.Projection.RIGID),
					1
				),
				Tuple.of(
					StructurePoolElement.single(
						villageType.getStructureResourceLocation("signpost").toString(),
						villageType.processorList
					).apply(StructureTemplatePool.Projection.TERRAIN_MATCHING),
					3
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
