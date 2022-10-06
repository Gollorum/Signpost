package gollorum.signpost.minecraft.worldgen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.mojang.datafixers.util.Pair;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallShortSignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallWideSignBlockPart;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.SignGeneratorBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.block.tiles.SignGeneratorEntity;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.utils.*;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VillageSignpost {

	private static final float smallSignRatio = 0.5f;
	private static Map<BlockPos, List<WaystoneHandle.Vanilla>> waystonesTargetedByVillage;
	public static void reset() {
		waystonesTargetedByVillage = new HashMap<>();
	}

	public static boolean populate(SignGeneratorEntity tile, ServerLevel level) {
		if(!Config.Server.worldGen.isVillageGenerationEnabled.get()) {
			return false;
		}
		BlockPos pieceLocation = tile.getBlockPos();
		level.setBlock(pieceLocation, PostBlock.OAK.getBlock().defaultBlockState(), 18);
		BlockPos villageLocation = VillageGenUtils.getVillageLocationFor(level, pieceLocation, 64);
		Random random = new Random(level.getSeed() ^ pieceLocation.asLong());
		Queue<Tuple<BlockPos, WaystoneHandle.Vanilla>> possibleTargets = fetchPossibleTargets(pieceLocation, villageLocation, random);
		if(possibleTargets.isEmpty())
			return false;

		Collection<WaystoneHandle.Vanilla> freshlyUsedWaystones = populateSignPostGeneration(
			tile.getBlockState().getValue(SignGeneratorBlock.FACING).getOpposite(), pieceLocation, level, random, possibleTargets
		);
		waystonesTargetedByVillage.computeIfAbsent(villageLocation, k -> new ArrayList<>())
			.addAll(freshlyUsedWaystones);
		return true;
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
		return VillageWaystone.getAllEntries().stream()
	        .filter(e -> !(e.getKey().equals(villageLocation)))
	        .map(Tuple::from);
	}
	private static Stream<Tuple<BlockPos, WaystoneHandle.Vanilla>> nonVillageWaystones() {
		return WaystoneLibrary.getInstance().getAllWaystoneInfo().stream()
			.map(info -> new Tuple<>(info.locationData.block.blockPos, info.handle))
			.filter(t -> VillageWaystone.getAllEntries().stream().noneMatch(e -> e.getValue().equals(t._2)));
	}

	private static Collection<WaystoneHandle.Vanilla> populateSignPostGeneration(
		Direction facing,
		BlockPos pieceLocation,
		ServerLevel level,
		Random random,
		Queue<Tuple<BlockPos, WaystoneHandle.Vanilla>> possibleTargets
	) {
		Tuple<Collection<WaystoneHandle.Vanilla>, Consumer<PostTile>> upperSignResult = makeSign(random, facing, level, pieceLocation,
			possibleTargets, 0.75f
		);
		Tuple<Collection<WaystoneHandle.Vanilla>, Consumer<PostTile>> lowerSignResult = makeSign(random, facing, level, pieceLocation,
			possibleTargets, 0.25f
		);
		TileEntityUtils.delayUntilTileEntityExists(level, pieceLocation, PostTile.getBlockEntityType(), tile -> {
			tile.addPart(
				new BlockPartInstance(new PostBlockPart(tile.modelType.postTexture), Vector3.ZERO),
				ItemStack.EMPTY,
				PlayerHandle.Invalid
			);
			upperSignResult._2.accept(tile);
			lowerSignResult._2.accept(tile);
			tile.setChanged();
		}, 20, Optional.of(() -> Signpost.LOGGER.error("Could not populate generated signpost at " + pieceLocation + ": TileEntity was not constructed.")));
		List<WaystoneHandle.Vanilla> ret = new ArrayList<>();
		ret.addAll(upperSignResult._1);
		ret.addAll(lowerSignResult._1);
		return ret;
	}

	public static Tuple<Collection<WaystoneHandle.Vanilla>, Consumer<PostTile>> makeSign(
		Random random,
		Direction facing,
		ServerLevel world,
		BlockPos tilePos,
		Queue<Tuple<BlockPos, WaystoneHandle.Vanilla>> possibleTargets,
		float y
	) {
		if(possibleTargets.isEmpty()) return new Tuple<>(Collections.emptySet(), x -> {});
		return random.nextFloat() < smallSignRatio
			? makeShortSigns(facing, world, tilePos, possibleTargets, y)
			: makeWideSign(facing, world, tilePos, possibleTargets, y);
	}

	private static Tuple<Collection<WaystoneHandle.Vanilla>, Consumer<PostTile>> makeWideSign(
		Direction facing,
		ServerLevel world,
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
						new AngleProvider.WaystoneTarget(rotation), new NameProvider.WaystoneTarget(targetData.name), shouldFlip(facing, rotation),
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

	private static Tuple<Collection<WaystoneHandle.Vanilla>, Consumer<PostTile>> makeShortSigns(
		Direction facing,
		ServerLevel world,
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
					new AngleProvider.WaystoneTarget(rotation), new NameProvider.WaystoneTarget(targetData.name), shouldFlip,
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
						new AngleProvider.WaystoneTarget(secondRotation), new NameProvider.WaystoneTarget(secondTargetData.name), shouldSecondFlip,
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

	private static Optional<Tuple<Tuple<BlockPos, WaystoneHandle.Vanilla>, WaystoneData>> fetchNextTarget(Queue<Tuple<BlockPos, WaystoneHandle.Vanilla>> possibleTargets) {
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

	private static Optional<Overlay> overlayFor(WorldGenLevel world, BlockPos pos) {
		Holder<Biome> biomeHolder = world.getBiome(pos);
		Biome biome = biomeHolder.value();
		Biome.BiomeCategory biomeCategory = Biome.getBiomeCategory(biomeHolder);
		if(biome.shouldSnow(world, pos)
			|| biome.getPrecipitation() == Biome.Precipitation.SNOW
			|| biomeCategory == Biome.BiomeCategory.ICY) return Optional.of(Overlay.Snow);
		else if (biomeCategory == Biome.BiomeCategory.JUNGLE) return Optional.of(Overlay.Vine);
		else if(biome.isHumid()) return Optional.of(Overlay.Gras);
		else return Optional.empty();
	}

}
