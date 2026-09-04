package gollorum.signpost.worldgen;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.worldgen.SignpostJigsawPiece;
import gollorum.signpost.minecraft.worldgen.VillageSignpost;
import gollorum.signpost.minecraft.worldgen.VillageWaystone;
import gollorum.signpost.minecraft.worldgen.WaystoneJigsawPiece;
import gollorum.signpost.mixin.ProcessorListsAccessor;
import gollorum.signpost.mixin.StructureTemplatePoolAccessor;
import gollorum.signpost.utils.Tuple;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Villages {

	public static final Villages instance = new Villages();
	private Villages() { /*VillagePools.bootstrap();*/ }

	@SuppressWarnings("UnreachableCode") // it thinks the accessor mixin throws
	private enum VillageType {
		Desert("desert", ProcessorListsAccessor.getEmpty()),
		Plains("plains", ProcessorLists.STREET_PLAINS),
		Savanna("savanna", ProcessorLists.STREET_SAVANNA),
		Snowy("snowy", ProcessorLists.STREET_SNOWY_OR_TAIGA),
		Taiga("taiga", ProcessorLists.STREET_SNOWY_OR_TAIGA);
		public final String name;
		public final ResourceKey<StructureProcessorList> processorList;

		VillageType(String name, ResourceKey<StructureProcessorList> processorList) {
			this.name = name;
			this.processorList = processorList;
		}

		public Identifier getSignpostStructureResourceLocation(String structureName) {
			return Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "village/" + name + "/" + structureName);
		}
	}

	private static final Map<ResourceKey<StructureTemplatePool>, ResourceKey<StructureTemplatePool>> waystonePoolByVillagePool =
		buildWaystonePools();

	private static Map<ResourceKey<StructureTemplatePool>, ResourceKey<StructureTemplatePool>> buildWaystonePools() {
		var map = new HashMap<ResourceKey<StructureTemplatePool>, ResourceKey<StructureTemplatePool>>();
		for(VillageType villageType : VillageType.values()) {
			map.put(poolKey(getVillagePool(villageType)), poolKey(getWaystonePool(villageType, false)));
			map.put(poolKey(getZombieVillagePool(villageType)), poolKey(getWaystonePool(villageType, true)));
		}
		return Map.copyOf(map);
	}

	/**
	 * The pool that holds nothing but this village type's waystone, if Signpost ships one for the given
	 * vanilla house pool. Used by {@link gollorum.signpost.mixin.JigsawPlacementPlacerInjector} to make the
	 * first house of every village a waystone.
	 */
	public static Optional<ResourceKey<StructureTemplatePool>> waystonePoolFor(ResourceKey<StructureTemplatePool> villagePool) {
		return Optional.ofNullable(waystonePoolByVillagePool.get(villagePool));
	}

	public static void reset() {
		VillageSignpost.reset();
		VillageWaystone.reset();
		SignpostJigsawPiece.reset();
		WaystoneJigsawPiece.reset();
	}

	public void initialize(RegistryAccess registryAccess) {
		var optionalPools = registryAccess.lookup(Registries.TEMPLATE_POOL);
		if(optionalPools.isEmpty()) {
			Signpost.LOGGER.error("Failed to initialize village generation: TemplatePool registry not found");
			return;
		}
		var optionalProcessorLists = registryAccess.lookup(Registries.PROCESSOR_LIST);
		if(optionalProcessorLists.isEmpty()) {
			Signpost.LOGGER.error("Failed to initialize village generation: ProcessorList registry not found");
			return;
		}
		var pools = optionalPools.get();
		var processorLists = optionalProcessorLists.get();
		for(VillageType villageType : VillageType.values()) {
			registerFor(villageType, true, pools, processorLists);
			registerFor(villageType, false, pools, processorLists);
		}
		reset();
	}

	/**
	 * The waystone is not registered here - it is forced into every village by
	 * {@link gollorum.signpost.mixin.JigsawPlacementPlacerInjector} instead, since a weighted entry among
	 * the ~87 weight worth of vanilla houses would only rarely be picked.
	 *
	 * <p>Processor lists are resolved from the registry of the server that is starting, every time. They
	 * must not be cached across servers: a {@link Holder} belongs to the registry it came from, and saving
	 * a piece that holds a stale one fails with "is not valid in current registry set", which aborts the
	 * write of the entire chunk.
	 */
	private void registerFor(
		VillageType villageType, boolean isZombie,
		Registry<StructureTemplatePool> pools, Registry<StructureProcessorList> processorLists
	) {
		Optional<? extends Holder<StructureProcessorList>> processors = processorLists.get(villageType.processorList);
		if(processors.isEmpty()) {
			Signpost.LOGGER.error("Tried to generate signposts in " + villageType.name
				+ " villages, but their processor list " + villageType.processorList.identifier() + " was not found in the registry.");
			return;
		}
		addToPool(
			ImmutableList.of(
				Tuple.of(
					new SignpostJigsawPiece(
						villageType.getSignpostStructureResourceLocation("signpost"),
						processors.get(),
						StructureTemplatePool.Projection.TERRAIN_MATCHING,
                        Optional.of(LiquidSettings.APPLY_WATERLOGGING),
						isZombie
					),
					3
				)
			),
			isZombie ? getZombieVillagePool(villageType) : getVillagePool(villageType),
			pools
		);
	}

	private static Identifier getVillagePool(VillageType villageType) {
		return Identifier.parse("village/" + villageType.name + "/houses");
	}

	private static Identifier getZombieVillagePool(VillageType villageType) {
		return Identifier.parse("village/" + villageType.name + "/zombie/houses");
	}

	private static Identifier getWaystonePool(VillageType villageType, boolean isZombie) {
		return Identifier.fromNamespaceAndPath(
			Signpost.MOD_ID,
			"village/" + villageType.name + (isZombie ? "/zombie" : "") + "/waystones"
		);
	}

	private static ResourceKey<StructureTemplatePool> poolKey(Identifier identifier) {
		return ResourceKey.create(Registries.TEMPLATE_POOL, identifier);
	}

	private void addToPool(
		Collection<Tuple<SinglePoolElement, Integer>> houses, Identifier poolId,
		Registry<StructureTemplatePool> registry
	) {
		var pool = registry.getValue(poolKey(poolId));
		if(pool == null) {
			Signpost.LOGGER.error("Tried to add elements to village pool " + poolId + ", but it was not found in the registry.");
			return;
		}
        var templatePool = (StructureTemplatePoolAccessor) pool;
		var templates = new ArrayList<>(templatePool.getRawTemplates());
		for(Tuple<SinglePoolElement, Integer> tuple : houses) {
			templates.add(new Pair<>(tuple._1(), tuple._2()));
			for(int i = 0; i < tuple._2(); i++) templatePool.getTemplates().add(tuple._1());
		}
        templatePool.setRawTemplates(templates);
	}

}
