package gollorum.signpost.minecraft.worldgen;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.Post;
import gollorum.signpost.blockpartdata.types.Sign;
import gollorum.signpost.blockpartdata.types.SmallShortSign;
import gollorum.signpost.blockpartdata.types.SmallWideSign;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.OwnershipData;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.utils.WaystoneData;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import javafx.util.Pair;
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

public class SignpostJigsawPiece extends SingleJigsawPiece {

	private static final float smallSignRatio = 0.5f;
	private static Map<BlockPos, List<WaystoneHandle>> waystonesTargetedByVillage;
	private static Map<BlockPos, Integer> signpostCountForVillage;
	public static void reset() {
		waystonesTargetedByVillage = new HashMap<>();
		signpostCountForVillage = new HashMap<>();
	}

	public static final Codec<SignpostJigsawPiece> codec = RecordCodecBuilder.create((codecBuilder) ->
		codecBuilder.group(func_236846_c_(), func_236844_b_(), func_236848_d_()).apply(codecBuilder, SignpostJigsawPiece::new));

	public SignpostJigsawPiece(
		ResourceLocation location,
		Supplier<StructureProcessorList> structureProcessorListSupplier,
		JigsawPattern.PlacementBehaviour placementBehaviour
	) {
		this(Either.left(location), structureProcessorListSupplier, placementBehaviour);
	}

	public SignpostJigsawPiece(
		Either<ResourceLocation, Template> template,
		Supplier<StructureProcessorList> structureProcessorListSupplier,
		JigsawPattern.PlacementBehaviour placementBehaviour
	) {
		super(template, structureProcessorListSupplier, placementBehaviour);
	}

	@Override
	public boolean func_230378_a_(
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
		Queue<Map.Entry<BlockPos, WaystoneHandle>> possibleTargets = fetchPossibleTargets(pieceLocation, villageLocation, random);
		if(possibleTargets.isEmpty()) return false;

		Template template = this.field_236839_c_.map(templateManager::getTemplateDefaulted, Function.identity());
		PlacementSettings placementSettings = this.func_230379_a_(rotation, boundingBox, shouldUseJigsawReplacementStructureProcessor);
		if (template.func_237146_a_(seedReader, pieceLocation, villageLocation, placementSettings, random, 18)) {
			Collection<WaystoneHandle> freshlyUsedWaystones = populateSignPostGeneration(
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

	private static Queue<Map.Entry<BlockPos, WaystoneHandle>> fetchPossibleTargets(BlockPos pieceLocation, BlockPos villageLocation, Random random) {
		return WaystoneJigsawPiece.getAllEntries().stream()
			.filter(e -> !(e.getKey().equals(villageLocation) || (
				waystonesTargetedByVillage.containsKey(villageLocation)
					&& waystonesTargetedByVillage.get(villageLocation).contains(e.getValue()))))
			.map(e -> new Pair<>(e, (float) Math.sqrt(e.getKey().distanceSq(pieceLocation)) * (0.5f + random.nextFloat())))
			.sorted((e1, e2) -> Float.compare(e1.getValue(), e2.getValue()))
			.map(Pair::getKey)
			.filter(e -> WaystoneLibrary.getInstance().contains(e.getValue()))
			.collect(Collectors.toCollection(LinkedList::new));
	}

	private Collection<WaystoneHandle> populateSignPostGeneration(
		PlacementSettings placementSettings,
		BlockPos pieceLocation,
		ISeedReader world,
		Random random,
		Queue<Map.Entry<BlockPos, WaystoneHandle>> possibleTargets
	) {
		Direction facing = placementSettings.getRotation().rotate(Direction.WEST);
		Direction left = placementSettings.getRotation().rotate(Direction.SOUTH);
		BlockPos lowerPos = pieceLocation.offset(facing.getOpposite()).offset(left).up();
		BlockPos upperPos = lowerPos.up();

		Pair<Collection<WaystoneHandle>, Consumer<PostTile>> upperSignResult = makeSign(random, facing, world, upperPos,
			possibleTargets, 0.75f
		);
		Pair<Collection<WaystoneHandle>, Consumer<PostTile>> lowerSignResult = makeSign(random, facing, world, upperPos,
			possibleTargets, 0.25f
		);
		TileEntityUtils.delayUntilTileEntityExists(world, upperPos, PostTile.class, tile -> {
			upperSignResult.getValue().accept(tile);
			lowerSignResult.getValue().accept(tile);
			tile.markDirty();
		}, 20, Optional.of(() -> Signpost.LOGGER.error("Could not populate generated signpost at " + upperPos + ": TileEntity was not constructed.")));
		List<WaystoneHandle> ret = new ArrayList<>();
		ret.addAll(upperSignResult.getKey());
		ret.addAll(lowerSignResult.getKey());
		return ret;
	}

	public static Pair<Collection<WaystoneHandle>, Consumer<PostTile>> makeSign(
		Random random,
		Direction facing,
		ISeedReader world,
		BlockPos tilePos,
		Queue<Map.Entry<BlockPos, WaystoneHandle>> possibleTargets,
		float y
	) {
		if(possibleTargets.isEmpty()) return new Pair<>(Collections.emptySet(), x -> {});
		return random.nextFloat() < smallSignRatio
			? makeShortSigns(facing, world, tilePos, possibleTargets, y)
			: makeWideSign(facing, world, tilePos, possibleTargets, y);
	}

	private static Pair<Collection<WaystoneHandle>, Consumer<PostTile>> makeWideSign(
		Direction facing,
		ISeedReader world,
		BlockPos tilePos,
		Queue<Map.Entry<BlockPos, WaystoneHandle>> possibleTargets,
		float y
	) {
		Map.Entry<BlockPos, WaystoneHandle> target = possibleTargets.poll();
		if(target == null) return new Pair<>(Collections.emptySet(), x -> {});
		WaystoneData targetData = WaystoneLibrary.getInstance().getData(target.getValue());
		Angle rotation = Sign.pointingAt(tilePos, target.getKey());
		Consumer<PostTile> onTileFetched = tile -> {
			if(tile.getParts().stream().anyMatch(instance -> !(instance.blockPart instanceof Post) && isNearly(instance.offset.y, y)))
				return;
			tile.addPart(
				new BlockPartInstance(
					new SmallWideSign(
						rotation, targetData.name, shouldFlip(facing, rotation),
						tile.modelType.mainTexture, tile.modelType.secondaryTexture,
						overlayFor(world, tilePos), Colors.black, Optional.of(target.getValue()),
						ItemStack.EMPTY, tile.modelType, false
					),
					new Vector3(0, y, 0)
				),
				ItemStack.EMPTY,
				PlayerHandle.Invalid
			);
		};
		return new Pair<>(Collections.singleton(target.getValue()), onTileFetched);
	}

	private static boolean isNearly(float a, float b) { return Math.abs(a - b) < 1e-5f; }

	private static Pair<Collection<WaystoneHandle>, Consumer<PostTile>> makeShortSigns(
		Direction facing,
		ISeedReader world,
		BlockPos tilePos,
		Queue<Map.Entry<BlockPos, WaystoneHandle>> possibleTargets,
		float y
	) {
		Map.Entry<BlockPos, WaystoneHandle> target = possibleTargets.poll();
		if(target == null) return new Pair<>(Collections.emptySet(), x -> {});
		WaystoneData targetData = WaystoneLibrary.getInstance().getData(target.getValue());
		Angle rotation = Sign.pointingAt(tilePos, target.getKey());
		boolean shouldFlip = shouldFlip(facing, rotation);
		Optional<Overlay> overlay = overlayFor(world, tilePos);
		List<Consumer<PostTile>> onTileFetched = new ArrayList<>();
		onTileFetched.add(tile -> tile.addPart(
			new BlockPartInstance(
				new SmallShortSign(
					rotation, targetData.name, shouldFlip,
					tile.modelType.mainTexture, tile.modelType.secondaryTexture,
					overlay, Colors.black, Optional.of(target.getValue()),
					ItemStack.EMPTY, tile.modelType, false
				),
				new Vector3(0, y, 0)
			),
			ItemStack.EMPTY,
			PlayerHandle.Invalid
		));
		Map.Entry<BlockPos, WaystoneHandle> secondTarget = possibleTargets.poll();
		List<Map.Entry<BlockPos, WaystoneHandle>> skippedTargets = new ArrayList<>();
		while(secondTarget != null) {
			WaystoneData secondTargetData = WaystoneLibrary.getInstance().getData(secondTarget.getValue());
			Angle secondRotation = Sign.pointingAt(tilePos, secondTarget.getKey());
			boolean shouldSecondFlip = shouldFlip(facing, secondRotation);
			if(shouldSecondFlip == shouldFlip) {
				skippedTargets.add(secondTarget);
				secondTarget = possibleTargets.poll();
				continue;
			}
			WaystoneHandle secondTargetHandle = secondTarget.getValue();
			onTileFetched.add(tile -> tile.addPart(
				new BlockPartInstance(
					new SmallShortSign(
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
		return new Pair<>(
			secondTarget == null
				? Collections.singleton(target.getValue())
				: ImmutableList.of(target.getValue(), secondTarget.getValue()),
			tile -> {
				if(!tile.getParts().stream().anyMatch(instance -> !(instance.blockPart instanceof Post) && isNearly(instance.offset.y, y)))
					for(Consumer<PostTile> now : onTileFetched) now.accept(tile);
			}
		);
	}

	private static boolean shouldFlip(Direction facing, Angle signRotation) {
		float degrees = signRotation.add(Angle.fromDegrees(facing.getHorizontalAngle())).normalized().degrees();
		return degrees < -90 || degrees > 90;
	}

	private static Optional<Overlay> overlayFor(ISeedReader world, BlockPos pos) {
		Biome biome = world.getBiome(pos);
		if(biome.doesSnowGenerate(world, pos)
			|| biome.getPrecipitation() == Biome.RainType.SNOW
			|| biome.getCategory() == Biome.Category.ICY) return Optional.of(Overlay.Snow);
		else if (biome.getCategory() == Biome.Category.JUNGLE) return Optional.of(Overlay.Vine);
		else if(biome.isHighHumidity()) return Optional.of(Overlay.Gras);
		else return Optional.empty();
	}

	public IJigsawDeserializer<?> getType() {
		return JigsawDeserializers.signpost;
	}

	public String toString() {
		return "SingleSignpost[" + this.field_236839_c_ + "]";
	}

}
