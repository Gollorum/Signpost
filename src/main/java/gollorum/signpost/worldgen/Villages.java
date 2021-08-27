package gollorum.signpost.worldgen;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.worldgen.SignpostJigsawPiece;
import gollorum.signpost.minecraft.worldgen.WaystoneJigsawPiece;
import gollorum.signpost.utils.CollectionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.gen.feature.jigsaw.JigsawPattern;
import net.minecraft.world.gen.feature.jigsaw.JigsawPiece;
import net.minecraft.world.gen.feature.jigsaw.LegacySingleJigsawPiece;
import net.minecraft.world.gen.feature.jigsaw.SingleJigsawPiece;
import net.minecraft.world.gen.feature.structure.VillagesPools;
import net.minecraft.world.gen.feature.template.ProcessorLists;
import net.minecraft.world.gen.feature.template.StructureProcessorList;
import net.minecraft.world.gen.feature.template.Template;

import java.util.*;
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

		public ResourceLocation getStructureResourceLocation(String structureName) {
			return new ResourceLocation(Signpost.MOD_ID, "village/" + name + "/" + structureName);
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
				Pair.of(
					new WaystoneJigsawPiece(villageType.getStructureResourceLocation("waystone"),
						villageType.processorList, JigsawPattern.PlacementBehaviour.RIGID),
					5
				),
				Pair.of(
					new SignpostJigsawPiece(villageType.getStructureResourceLocation("signpost"),
						villageType.processorList, JigsawPattern.PlacementBehaviour.TERRAIN_MATCHING, isZombie),
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

	private void addLocationsToPool(
		Collection<Pair<ResourceLocation, Integer>> houses, JigsawPattern.PlacementBehaviour placementBehaviour, StructureProcessorList processorList, ResourceLocation pool
	) {
		addToPool(houses.stream().map(loc -> Pair.of(jigsawPieceFrom(loc.getFirst(), () -> processorList, placementBehaviour), loc.getSecond()))
			.collect(Collectors.toList()), pool);
	}

	private SingleJigsawPiece jigsawPieceFrom(ResourceLocation loc, Supplier<StructureProcessorList> processorList, JigsawPattern.PlacementBehaviour placementBehaviour) {
		return LegacySingleJigsawPiece.func_242851_a(loc.toString(), processorList.get()).apply(placementBehaviour);
	}

	private SingleJigsawPiece jigsawPieceFrom(Template template) { return new SingleJigsawPiece(template); }

	private void addTemplatesToPool(
		Collection<Pair<Template, Integer>> houses, JigsawPattern.PlacementBehaviour placementBehaviour, StructureProcessorList processorList, ResourceLocation pool
	) {
		addToPool(houses.stream()
			.map(pair -> Pair.of(jigsawPieceFrom(pair.getFirst()), pair.getSecond()))
			.collect(Collectors.toList()), pool);
	}

	private void addToPool(
		Collection<Pair<SingleJigsawPiece, Integer>> houses, ResourceLocation pool
	) {
		JigsawPattern oldPattern = WorldGenRegistries.JIGSAW_POOL.getOrDefault(pool);
		if(oldPattern == null) {
			Signpost.LOGGER.error("Tried to add elements to village pool " + pool + ", but it was not found in the registry.");
			return;
		}
		Map<JigsawPiece, Integer> allPieces = CollectionUtils.group(oldPattern.getShuffledPieces(new Random(0)));
		for(Pair<SingleJigsawPiece, Integer> pair : houses) {
			allPieces.put(pair.getFirst(), pair.getSecond());
		}
		registerPoolEntries(allPieces, pool, oldPattern.getName());
	}

	private void removeSignpostPiecesFromPool(ResourceLocation pool) {
		JigsawPattern oldPattern = WorldGenRegistries.JIGSAW_POOL.getOrDefault(pool);
		if(oldPattern == null) {
			Signpost.LOGGER.error("Tried to remove elements from village pool " + pool + ", but it was not found in the registry.");
			return;
		}
		Map<JigsawPiece, Integer> allPieces = CollectionUtils.group(
			oldPattern.getShuffledPieces(new Random(0))
				.stream().filter(piece -> !(piece instanceof SignpostJigsawPiece || piece instanceof WaystoneJigsawPiece))
				.collect(Collectors.toCollection(ArrayList<JigsawPiece>::new)));
		registerPoolEntries(allPieces, pool, oldPattern.getName());
	}

	private static void registerPoolEntries(Map<JigsawPiece, Integer> pieces, ResourceLocation pool, ResourceLocation patternName) {
		Registry.register(WorldGenRegistries.JIGSAW_POOL, pool, new JigsawPattern(pool, patternName, pieces.entrySet().stream().map(e -> Pair
			.of(e.getKey(), e.getValue())).collect(Collectors.toList())));
	}

	public static Template getTemplate(ResourceLocation loc) {
		return Signpost.getServerInstance().getTemplateManager().getTemplate(loc);
	}

}
