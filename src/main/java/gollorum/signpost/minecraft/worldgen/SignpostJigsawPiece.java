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
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.jigsaw.IJigsawDeserializer;
import net.minecraft.world.gen.feature.jigsaw.JigsawPattern;
import net.minecraft.world.gen.feature.jigsaw.SingleJigsawPiece;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.StructureProcessorList;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.gen.feature.template.TemplateManager;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SignpostJigsawPiece extends SingleJigsawPiece {

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
		JigsawPattern.PlacementBehaviour placementBehaviour,
		boolean isZombie
	) {
		this(Either.left(location), structureProcessorListSupplier, placementBehaviour, isZombie);
	}

	public SignpostJigsawPiece(
		Either<ResourceLocation, Template> template,
		Supplier<StructureProcessorList> structureProcessorListSupplier,
		JigsawPattern.PlacementBehaviour placementBehaviour,
		boolean isZombie
	) {
		super(template, structureProcessorListSupplier, placementBehaviour);
		this.isZombie = isZombie;
	}

	@Override
	public boolean place(
		TemplateManager templateManager,
		ISeedReader seedReader,
		StructureManager structureManager,
		ChunkGenerator chunkGenerator,
		BlockPos pieceLocation,
		BlockPos villageLocation,
		Rotation rotation,
		MutableBoundingBox boundingBox,
		Random random,
		boolean shouldUseJigsawReplacementStructureProcessor
	) {
		if(!Config.Server.worldGen.isVillageGenerationEnabled.get()) return false;
		if(signpostCountForVillage.getOrDefault(villageLocation, 0) >= Config.Server.worldGen.maxSignpostsPerVillage.get())
			return false;
		Queue<Tuple<BlockPos, WaystoneHandle.Vanilla>> possibleTargets = fetchPossibleTargets(pieceLocation, villageLocation, random);
		if(possibleTargets.isEmpty()) return false;

		Template template = this.template.map(templateManager::get, Function.identity());
		PlacementSettings placementSettings = this.getSettings(rotation, boundingBox, shouldUseJigsawReplacementStructureProcessor);
		if (template.placeInWorld(seedReader, pieceLocation, villageLocation, placementSettings, random, 18)) {
			Collection<WaystoneHandle.Vanilla> freshlyUsedWaystones = populateSignPostGeneration(
				placementSettings, pieceLocation, seedReader, random, possibleTargets
			);
			waystonesTargetedByVillage.computeIfAbsent(villageLocation, k -> new ArrayList<>())
				.addAll(freshlyUsedWaystones);

			for(Template.BlockInfo blockInfo : Template.processBlockInfos(
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
		PlacementSettings placementSettings,
		BlockPos pieceLocation,
		ISeedReader world,
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
		ISeedReader world,
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
		ISeedReader world,
		BlockPos tilePos,
		Queue<Tuple<BlockPos, WaystoneHandle.Vanilla>> possibleTargets,
		float y
	) {
		Tuple<BlockPos, WaystoneHandle.Vanilla> target = possibleTargets.poll();
		if(target == null) return new Tuple<>(Collections.emptySet(), x -> {});
		WaystoneData targetData = WaystoneLibrary.getInstance().getData(target._2);
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
		ISeedReader world,
		BlockPos tilePos,
		Queue<Tuple<BlockPos, WaystoneHandle.Vanilla>> possibleTargets,
		float y
	) {
		Tuple<BlockPos, WaystoneHandle.Vanilla> target = possibleTargets.poll();
		if(target == null) return new Tuple<>(Collections.emptySet(), x -> {});
		WaystoneData targetData = WaystoneLibrary.getInstance().getData(target._2);
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
		Tuple<BlockPos, WaystoneHandle.Vanilla> secondTarget = possibleTargets.poll();
		List<Tuple<BlockPos, WaystoneHandle.Vanilla>> skippedTargets = new ArrayList<>();
		while(secondTarget != null) {
			WaystoneData secondTargetData = WaystoneLibrary.getInstance().getData(secondTarget._2);
			Angle secondRotation = SignBlockPart.pointingAt(tilePos, secondTarget._1);
			boolean shouldSecondFlip = shouldFlip(facing, secondRotation);
			if(shouldSecondFlip == shouldFlip) {
				skippedTargets.add(secondTarget);
				secondTarget = possibleTargets.poll();
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

	private static boolean shouldFlip(Direction facing, Angle signRotation) {
		float degrees = signRotation.add(Angle.fromDegrees(facing.toYRot())).normalized().degrees();
		return degrees < -90 || degrees > 90;
	}

	private Optional<Overlay> overlayFor(ISeedReader world, BlockPos pos) {
		Biome biome = world.getBiome(pos);
		if(biome.shouldSnow(world, pos)
			|| biome.getPrecipitation() == Biome.RainType.SNOW
			|| biome.getBiomeCategory() == Biome.Category.ICY) return Optional.of(Overlay.Snow);
		else if (biome.getBiomeCategory() == Biome.Category.JUNGLE) return Optional.of(Overlay.Vine);
		else if(biome.isHumid() || isZombie) return Optional.of(Overlay.Gras);
		else return Optional.empty();
	}

	public IJigsawDeserializer<?> getType() {
		return JigsawDeserializers.signpost;
	}

	public String toString() {
		return "SingleSignpost[" + this.template + "]";
	}

}
