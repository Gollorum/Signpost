package gollorum.signpost.minecraft.worldgen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallShortSignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallWideSignBlockPart;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.Tuple;
import gollorum.signpost.utils.WaystoneData;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.structures.SinglePoolElement;
import net.minecraft.world.level.levelgen.feature.structures.StructurePoolElementType;
import net.minecraft.world.level.levelgen.feature.structures.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SignpostJigsawPiece extends SinglePoolElement {

	private static final float smallSignRatio = 0.5f;
	private static Map<BlockPos, List<WaystoneHandle.Vanilla>> waystonesTargetedByVillage;
	private static Map<BlockPos, Integer> signpostCountForVillage;
	public static void reset() {
		waystonesTargetedByVillage = new HashMap<>();
		signpostCountForVillage = new HashMap<>();
	}

	public static final Codec<SignpostJigsawPiece> codec = RecordCodecBuilder.create((codecBuilder) ->
		codecBuilder.group(templateCodec(), processorsCodec(), projectionCodec(), isZombieCodec()).apply(codecBuilder, SignpostJigsawPiece::new));

	private static RecordCodecBuilder<SignpostJigsawPiece, Boolean> isZombieCodec() {
		return Codec.BOOL.fieldOf("isZombie").forGetter(o -> o.isZombie);
	}

	public final boolean isZombie;

	public SignpostJigsawPiece(
		ResourceLocation location,
		Supplier<StructureProcessorList> structureProcessorListSupplier,
		StructureTemplatePool.Projection placementBehaviour,
		boolean isZombie
	) {
		this(Either.left(location), structureProcessorListSupplier, placementBehaviour, isZombie);
	}

	public SignpostJigsawPiece(
		Either<ResourceLocation, StructureTemplate> template,
		Supplier<StructureProcessorList> structureProcessorListSupplier,
		StructureTemplatePool.Projection placementBehaviour,
		boolean isZombie
	) {
		super(template, structureProcessorListSupplier, placementBehaviour);
		this.isZombie = isZombie;
	}

	@Override
	public boolean place(
		StructureManager templateManager,
		WorldGenLevel seedReader,
		StructureFeatureManager structureManager,
		ChunkGenerator chunkGenerator,
		BlockPos pieceLocation,
		BlockPos villageLocation,
		Rotation rotation,
		BoundingBox boundingBox,
		Random random,
		boolean shouldUseJigsawReplacementStructureProcessor
	) {
		if(!Config.Server.worldGen.isVillageGenerationEnabled.get()) return false;
		if(signpostCountForVillage.getOrDefault(villageLocation, 0) >= Config.Server.worldGen.maxSignpostsPerVillage.get())
			return false;
		Queue<Tuple<BlockPos, WaystoneHandle.Vanilla>> possibleTargets = fetchPossibleTargets(pieceLocation, villageLocation, random);
		if(possibleTargets.isEmpty()) {
			Signpost.LOGGER.debug("Did not generate signpost because no targets were found.");
			return false;
		}

		StructureTemplate template = this.template.map(templateManager::getOrCreate, Function.identity());
		StructurePlaceSettings placementSettings = this.getSettings(rotation, boundingBox, shouldUseJigsawReplacementStructureProcessor);
		if (template.placeInWorld(seedReader, pieceLocation, villageLocation, placementSettings, random, 18)) {
			Collection<WaystoneHandle.Vanilla> freshlyUsedWaystones = populateSignPostGeneration(
				placementSettings, pieceLocation, seedReader, random, possibleTargets
			);
			waystonesTargetedByVillage.computeIfAbsent(villageLocation, k -> new ArrayList<>())
				.addAll(freshlyUsedWaystones);

			for(StructureTemplate.StructureBlockInfo blockInfo : StructureTemplate.processBlockInfos(
				seedReader, pieceLocation, villageLocation, placementSettings,
				this.getDataMarkers(templateManager, pieceLocation, rotation, false), template
			)) {
				this.handleDataMarker(seedReader, blockInfo, pieceLocation, rotation, random, boundingBox);
			}

			signpostCountForVillage.put(villageLocation, signpostCountForVillage.getOrDefault(villageLocation, 0) + 1);
			return true;
		} else {
			return false;
		}
	}

	private static Queue<Tuple<BlockPos, WaystoneHandle.Vanilla>> fetchPossibleTargets(BlockPos pieceLocation, BlockPos villageLocation, Random random) {
		return allWaystoneTargets(villageLocation)
			.map(e -> new Tuple<>(e, (float) Math.sqrt(e._1.distSqr(pieceLocation)) * (0.5f + random.nextFloat())))
			.sorted((e1, e2) -> Float.compare(e1._2, e2._2))
			.map(Tuple::getLeft)
			.filter(e -> WaystoneLibrary.getInstance().contains(e._2))
			.collect(Collectors.toCollection(LinkedList::new));
	}

	private static Stream<Tuple<BlockPos, WaystoneHandle.Vanilla>> allWaystoneTargets(BlockPos villageLocation) {
		Stream<Tuple<BlockPos, WaystoneHandle.Vanilla>> villageWaystones = villageWaystonesExceptSelf(villageLocation);
		return (Config.Server.worldGen.villagesOnlyTargetVillages.get()
			? villageWaystones
			: Streams.concat(villageWaystones, nonVillageWaystones())
		).filter(e -> !(waystonesTargetedByVillage.containsKey(villageLocation)
			&& waystonesTargetedByVillage.get(villageLocation).contains(e._2)));
	}
	private static Stream<Tuple<BlockPos, WaystoneHandle.Vanilla>> villageWaystonesExceptSelf(BlockPos villageLocation) {
		return WaystoneJigsawPiece.getAllEntries().stream()
	        .filter(e -> !(e.getKey().equals(villageLocation)))
	        .map(Tuple::from);
	}
	private static Stream<Tuple<BlockPos, WaystoneHandle.Vanilla>> nonVillageWaystones() {
		return WaystoneLibrary.getInstance().getAllWaystoneInfo().stream()
			.map(info -> new Tuple<>(info.locationData.block.blockPos, info.handle))
			.filter(t -> WaystoneJigsawPiece.getAllEntries().stream().noneMatch(e -> e.getValue().equals(t._2)));
	}

	private Collection<WaystoneHandle.Vanilla> populateSignPostGeneration(
		StructurePlaceSettings placementSettings,
		BlockPos pieceLocation,
		WorldGenLevel world,
		Random random,
		Queue<Tuple<BlockPos, WaystoneHandle.Vanilla>> possibleTargets
	) {
		Direction facing = placementSettings.getRotation().rotate(Direction.WEST);
		Direction left = placementSettings.getRotation().rotate(Direction.SOUTH);
		BlockPos lowerPos = pieceLocation.relative(facing.getOpposite()).relative(left).above();
		BlockPos upperPos = lowerPos.above();

		Tuple<Collection<WaystoneHandle.Vanilla>, Consumer<PostTile>> upperSignResult = makeSign(random, facing, world, upperPos,
			possibleTargets, 0.75f
		);
		Tuple<Collection<WaystoneHandle.Vanilla>, Consumer<PostTile>> lowerSignResult = makeSign(random, facing, world, upperPos,
			possibleTargets, 0.25f
		);
		TileEntityUtils.delayUntilTileEntityExists(world.getLevel(), upperPos, PostTile.class, tile -> {
			upperSignResult._2.accept(tile);
			lowerSignResult._2.accept(tile);
			tile.setChanged();
		}, 20, Optional.of(() -> Signpost.LOGGER.error("Could not populate generated signpost at " + upperPos + ": TileEntity was not constructed.")));
		List<WaystoneHandle.Vanilla> ret = new ArrayList<>();
		ret.addAll(upperSignResult._1);
		ret.addAll(lowerSignResult._1);
		return ret;
	}

	public Tuple<Collection<WaystoneHandle.Vanilla>, Consumer<PostTile>> makeSign(
		Random random,
		Direction facing,
		WorldGenLevel world,
		BlockPos tilePos,
		Queue<Tuple<BlockPos, WaystoneHandle.Vanilla>> possibleTargets,
		float y
	) {
		if(possibleTargets.isEmpty()) return new Tuple<>(Collections.emptySet(), x -> {});
		return random.nextFloat() < smallSignRatio
			? makeShortSigns(facing, world, tilePos, possibleTargets, y)
			: makeWideSign(facing, world, tilePos, possibleTargets, y);
	}

	private Tuple<Collection<WaystoneHandle.Vanilla>, Consumer<PostTile>> makeWideSign(
		Direction facing,
		WorldGenLevel world,
		BlockPos tilePos,
		Queue<Tuple<BlockPos, WaystoneHandle.Vanilla>> possibleTargets,
		float y
	) {
		Optional<Tuple<Tuple<BlockPos, WaystoneHandle.Vanilla>, WaystoneData>> nextTargetOption = fetchNextTarget(possibleTargets);
		if(nextTargetOption.isEmpty()) return new Tuple<>(Collections.emptySet(), x -> {});
		Tuple<BlockPos, WaystoneHandle.Vanilla> target = nextTargetOption.get()._1;
		WaystoneData targetData = nextTargetOption.get()._2;

		Angle rotation = SignBlockPart.pointingAt(tilePos, target._1);
		Consumer<PostTile> onTileFetched = tile -> {
			if(tile.getParts().stream().anyMatch(instance -> !(instance.blockPart instanceof PostBlockPart) && isNearly(instance.offset.y, y)))
				return;
			tile.addPart(
				new BlockPartInstance(
					new SmallWideSignBlockPart(
						rotation, targetData.name, shouldFlip(facing, rotation),
						tile.modelType.mainTexture, tile.modelType.secondaryTexture,
						overlayFor(world, tilePos), Colors.black, Optional.of(target._2),
						ItemStack.EMPTY, tile.modelType, false
					),
					new Vector3(0, y, 0)
				),
				ItemStack.EMPTY,
				PlayerHandle.Invalid
			);
		};
		return new Tuple<>(Collections.singleton(target._2), onTileFetched);
	}

	private static boolean isNearly(float a, float b) { return Math.abs(a - b) < 1e-5f; }

	private Tuple<Collection<WaystoneHandle.Vanilla>, Consumer<PostTile>> makeShortSigns(
		Direction facing,
		WorldGenLevel world,
		BlockPos tilePos,
		Queue<Tuple<BlockPos, WaystoneHandle.Vanilla>> possibleTargets,
		float y
	) {
		Optional<Tuple<Tuple<BlockPos, WaystoneHandle.Vanilla>, WaystoneData>> nextTargetOption = fetchNextTarget(possibleTargets);
		if(nextTargetOption.isEmpty()) return new Tuple<>(Collections.emptySet(), x -> {});
		Tuple<BlockPos, WaystoneHandle.Vanilla> target = nextTargetOption.get()._1;
		WaystoneData targetData = nextTargetOption.get()._2;

		Angle rotation = SignBlockPart.pointingAt(tilePos, target._1);
		boolean shouldFlip = shouldFlip(facing, rotation);
		Optional<Overlay> overlay = overlayFor(world, tilePos);
		List<Consumer<PostTile>> onTileFetched = new ArrayList<>();
		onTileFetched.add(tile -> tile.addPart(
			new BlockPartInstance(
				new SmallShortSignBlockPart(
					rotation, targetData.name, shouldFlip,
					tile.modelType.mainTexture, tile.modelType.secondaryTexture,
					overlay, Colors.black, Optional.of(target._2),
					ItemStack.EMPTY, tile.modelType, false
				),
				new Vector3(0, y, 0)
			),
			ItemStack.EMPTY,
			PlayerHandle.Invalid
		));

		Optional<Tuple<Tuple<BlockPos, WaystoneHandle.Vanilla>, WaystoneData>> secondNextTargetOption = fetchNextTarget(possibleTargets);
		if(secondNextTargetOption.isEmpty()) return new Tuple<>(Collections.emptySet(), x -> {});
		Tuple<BlockPos, WaystoneHandle.Vanilla> secondTarget = secondNextTargetOption.get()._1;

		List<Tuple<BlockPos, WaystoneHandle.Vanilla>> skippedTargets = new ArrayList<>();
		while(secondTarget != null) {
			WaystoneData secondTargetData = secondNextTargetOption.get()._2;
			Angle secondRotation = SignBlockPart.pointingAt(tilePos, secondTarget._1);
			boolean shouldSecondFlip = shouldFlip(facing, secondRotation);
			if(shouldSecondFlip == shouldFlip) {
				skippedTargets.add(secondTarget);
				secondNextTargetOption = fetchNextTarget(possibleTargets);
				secondTarget = secondNextTargetOption.isEmpty() ? null : secondNextTargetOption.get()._1;
				continue;
			}
			WaystoneHandle.Vanilla secondTargetHandle = secondTarget._2;
			onTileFetched.add(tile -> tile.addPart(
				new BlockPartInstance(
					new SmallShortSignBlockPart(
						secondRotation, secondTargetData.name, shouldSecondFlip,
						tile.modelType.mainTexture, tile.modelType.secondaryTexture,
						overlay, Colors.black, Optional.of(secondTargetHandle),
						ItemStack.EMPTY, tile.modelType, false
					),
					new Vector3(0, y, 0)
				),
				ItemStack.EMPTY,
				PlayerHandle.Invalid
			));
			break;
		}
		skippedTargets.addAll(possibleTargets);
		possibleTargets.clear();
		possibleTargets.addAll(skippedTargets);
		return new Tuple<>(
			secondTarget == null
				? Collections.singleton(target._2)
				: ImmutableList.of(target._2, secondTarget._2),
			tile -> {
				if(tile.getParts().stream().noneMatch(instance -> !(instance.blockPart instanceof PostBlockPart) && isNearly(instance.offset.y, y)))
					for(Consumer<PostTile> now : onTileFetched) now.accept(tile);
			}
		);
	}

	private Optional<Tuple<Tuple<BlockPos, WaystoneHandle.Vanilla>, WaystoneData>> fetchNextTarget(Queue<Tuple<BlockPos, WaystoneHandle.Vanilla>> possibleTargets) {
		Tuple<BlockPos, WaystoneHandle.Vanilla> target = null;
		WaystoneData targetData = null;
		while(target == null && possibleTargets.size() > 0) {
			target = possibleTargets.poll();
			if(target == null) continue;
			Optional<WaystoneData> dataOptional = WaystoneLibrary.getInstance().getData(target._2);
			if(dataOptional.isPresent()) targetData = dataOptional.get();
			else target = null;
		}
		return target == null ? Optional.empty() : Optional.of(Tuple.of(target, targetData));
	}

	private static boolean shouldFlip(Direction facing, Angle signRotation) {
		float degrees = signRotation.add(Angle.fromDegrees(facing.toYRot())).normalized().degrees();
		return degrees < -90 || degrees > 90;
	}

	private Optional<Overlay> overlayFor(WorldGenLevel world, BlockPos pos) {
		Biome biome = world.getBiome(pos);
		if(biome.shouldSnow(world, pos)
			|| biome.getPrecipitation() == Biome.Precipitation.SNOW
			|| biome.getBiomeCategory() == Biome.BiomeCategory.ICY) return Optional.of(Overlay.Snow);
		else if (biome.getBiomeCategory() == Biome.BiomeCategory.JUNGLE) return Optional.of(Overlay.Vine);
		else if(biome.isHumid() || isZombie) return Optional.of(Overlay.Gras);
		else return Optional.empty();
	}

	public StructurePoolElementType<?> getType() {
		return JigsawDeserializers.signpost;
	}

	public String toString() {
		return "SingleSignpost[" + this.template + "]";
	}

}
