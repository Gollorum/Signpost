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
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.data.worldgen.VillagePools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraftforge.fml.ModList;

import java.util.Collection;

public class Villages {

	public static final Villages instance = new Villages();
	private Villages() { VillagePools.bootstrap(); }

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

	private void registerProcessorLists() {
		waystoneProcessorListDesert = ProcessorLists.EMPTY;
		waystoneProcessorListPlains = ProcessorLists.STREET_PLAINS;
		waystoneProcessorListSavanna = ProcessorLists.STREET_SAVANNA;
		waystoneProcessorListSnowyOrTaiga = ProcessorLists.STREET_SNOWY_OR_TAIGA;
	}

	public static void reset() {
		VillageSignpost.reset();
		VillageWaystone.reset();
		SignpostJigsawPiece.reset();
		WaystoneJigsawPiece.reset();
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
			isZombie ? getZombieVillagePool(villageType) : getVillagePool(villageType)
		);
	}

	private static ResourceLocation getVillagePool(VillageType villageType) {
		return new ResourceLocation("village/" + villageType.name + "/houses");
	}

	private static ResourceLocation getZombieVillagePool(VillageType villageType) {
		return new ResourceLocation("village/" + villageType.name + "/zombie/houses");
	}

	private void addToPool(
		Collection<Tuple<SinglePoolElement, Integer>> houses, ResourceLocation poolKey
	) {
		StructureTemplatePool pool = BuiltinRegistries.TEMPLATE_POOL.get(poolKey);
		if(pool == null) {
			Signpost.LOGGER.error("Tried to add elements to village pool " + poolKey + ", but it was not found in the registry.");
			return;
		}
		for(Tuple<SinglePoolElement, Integer> tuple : houses) {
			pool.rawTemplates.add(new Pair<>(tuple._1, tuple._2));
			for(int i = 0; i < tuple._2; i++) pool.templates.add(tuple._1);
		}
	}

}
