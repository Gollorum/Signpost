package gollorum.signpost.worldgen;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.worldgen.SignpostJigsawPiece;
import gollorum.signpost.minecraft.worldgen.VillageSignpost;
import gollorum.signpost.minecraft.worldgen.VillageWaystone;
import gollorum.signpost.minecraft.worldgen.WaystoneJigsawPiece;
import gollorum.signpost.utils.Tuple;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.ArrayList;
import java.util.Collection;

public class Villages {

	public static final Villages instance = new Villages();
	private Villages() { /*VillagePools.bootstrap();*/ }

	private enum VillageType {
		Desert("desert", instance.waystoneProcessorListDesert, false),
		Plains("plains", instance.waystoneProcessorListPlains, true),
		Savanna("savanna", instance.waystoneProcessorListSavanna, true),
		Snowy("snowy", instance.waystoneProcessorListSnowyOrTaiga, true),
		Taiga("taiga", instance.waystoneProcessorListSnowyOrTaiga, true);
		public final String name;
		public final Holder<StructureProcessorList> processorList;
		public final boolean isCommonGround;

		VillageType(String name, Holder<StructureProcessorList> processorList, boolean isCommonGround) {
			this.name = name;
			this.processorList = processorList;
			this.isCommonGround = isCommonGround;
		}

		public ResourceLocation getSignpostStructureResourceLocation(String structureName) {
			return new ResourceLocation(Signpost.MOD_ID, "village/" + name + "/" + structureName);
		}

		public ResourceLocation getWaystoneStructureResourceLocation(String structureName) {
			return new ResourceLocation(Signpost.MOD_ID, "village/" + (isCommonGround ? "common" : name) + "/" + structureName);
		}
	}

	private Holder<StructureProcessorList> waystoneProcessorListDesert;
	private Holder<StructureProcessorList> waystoneProcessorListPlains;
	private Holder<StructureProcessorList> waystoneProcessorListSavanna;
	private Holder<StructureProcessorList> waystoneProcessorListSnowyOrTaiga;

	private void registerProcessorLists(RegistryAccess registryAccess) {
		var optionalReg = registryAccess.registry(Registries.PROCESSOR_LIST);
		if(optionalReg.isEmpty()) {
			Signpost.LOGGER.error("Failed to initialize village generation: ProcessorList registry not found");
			return;
		}
		var reg = optionalReg.get();
		waystoneProcessorListDesert = reg.getHolderOrThrow(ProcessorLists.EMPTY);
		waystoneProcessorListPlains = reg.getHolderOrThrow(ProcessorLists.STREET_PLAINS);
		waystoneProcessorListSavanna = reg.getHolderOrThrow(ProcessorLists.STREET_SAVANNA);
		waystoneProcessorListSnowyOrTaiga = reg.getHolderOrThrow(ProcessorLists.STREET_SNOWY_OR_TAIGA);
	}

	public static void reset() {
		VillageSignpost.reset();
		VillageWaystone.reset();
		SignpostJigsawPiece.reset();
		WaystoneJigsawPiece.reset();
	}

	public void initialize(RegistryAccess registryAccess) {
		registerProcessorLists(registryAccess);

		var optionalReg = registryAccess.registry(Registries.TEMPLATE_POOL);
		if(optionalReg.isEmpty()) {
			Signpost.LOGGER.error("Failed to initialize village generation: TemplatePool registry not found");
			return;
		}
		var reg = optionalReg.get();
		for(VillageType villageType : VillageType.values()) {
			registerFor(villageType, true, reg);
			registerFor(villageType, false, reg);
		}
		reset();
	}

	private void registerFor(VillageType villageType, boolean isZombie, Registry<StructureTemplatePool> registry) {
		addToPool(
			ImmutableList.of(
				Tuple.of(
					new WaystoneJigsawPiece(
						villageType.getWaystoneStructureResourceLocation("waystone"),
					    villageType.processorList,
						StructureTemplatePool.Projection.RIGID
					),
					1
				),
				Tuple.of(
					new SignpostJigsawPiece(
						villageType.getSignpostStructureResourceLocation("signpost"),
						villageType.processorList,
						StructureTemplatePool.Projection.TERRAIN_MATCHING,
						isZombie
					),
					3
				)
			),
			isZombie ? getZombieVillagePool(villageType) : getVillagePool(villageType),
			registry
		);
	}

	private static ResourceLocation getVillagePool(VillageType villageType) {
		return new ResourceLocation("village/" + villageType.name + "/houses");
	}

	private static ResourceLocation getZombieVillagePool(VillageType villageType) {
		return new ResourceLocation("village/" + villageType.name + "/zombie/houses");
	}

	private void addToPool(
		Collection<Tuple<SinglePoolElement, Integer>> houses, ResourceLocation poolKey,
		Registry<StructureTemplatePool> registry
	) {
		var key = ResourceKey.create(Registries.TEMPLATE_POOL, poolKey);
		var poolHolder = registry.getHolder(key);
		if(poolHolder.isEmpty()) {
			Signpost.LOGGER.error("Tried to add elements to village pool " + poolKey + ", but it was not found in the registry.");
			return;
		}
		var pool = poolHolder.get().get();
		var templates = new ArrayList<>(pool.rawTemplates);
		for(Tuple<SinglePoolElement, Integer> tuple : houses) {
			templates.add(new Pair<>(tuple._1, tuple._2));
			for(int i = 0; i < tuple._2; i++) pool.templates.add(tuple._1);
		}
		pool.rawTemplates = templates;
	}

}
