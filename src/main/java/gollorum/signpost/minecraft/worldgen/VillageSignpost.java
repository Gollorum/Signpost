package gollorum.signpost.minecraft.worldgen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallShortSignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallWideSignBlockPart;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.utils.*;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;

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

	public static boolean populate(PostTile tile, SignBlockPart<?> generatorPart, UUID generatorPartId, float height, ServerLevel level) {
		if(!Config.Server.worldGen.isVillageGenerationEnabled.get()) {
			return false;
		}
		BlockPos pieceLocation = tile.getBlockPos();
		BlockPos villageLocation = VillageGenUtils.getVillageLocationFor(level, pieceLocation, 64);
		Random random = new Random(level.getSeed() ^ pieceLocation.asLong());
		Queue<Tuple<BlockPos, WaystoneHandle.Vanilla>> possibleTargets = fetchPossibleTargets(pieceLocation, villageLocation, random);
		if(possibleTargets.isEmpty())
			return false;

		Collection<WaystoneHandle.Vanilla> freshlyUsedWaystones = populateSignPostGeneration(
			tile, generatorPart, height,
			tile.getBlockState().getValue(PostBlock.FACING).getOpposite(), pieceLocation, level, random, possibleTargets
		);
		waystonesTargetedByVillage.computeIfAbsent(villageLocation, k -> new ArrayList<>())
			.addAll(freshlyUsedWaystones);
		tile.removePart(generatorPartId);
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
		PostTile tile,
		SignBlockPart<?> generatorPart,
		float height,
		Direction facing,
		BlockPos pieceLocation,
		ServerLevel level,
		Random random,
		Queue<Tuple<BlockPos, WaystoneHandle.Vanilla>> possibleTargets
	) {
		return makeSign(tile, generatorPart, random, facing, level, pieceLocation, possibleTargets, height);
	}

	public static Collection<WaystoneHandle.Vanilla> makeSign(
		PostTile tile,
		SignBlockPart<?> generatorPart,
		Random random,
		Direction facing,
		ServerLevel world,
		BlockPos tilePos,
		Queue<Tuple<BlockPos, WaystoneHandle.Vanilla>> possibleTargets,
		float y
	) {
		if(possibleTargets.isEmpty()) return Collections.emptySet();
		return random.nextFloat() < smallSignRatio
			? makeShortSigns(tile, generatorPart, facing, world, tilePos, possibleTargets, y)
			: makeWideSign(tile, generatorPart, facing, world, tilePos, possibleTargets, y);
	}

	private static Collection<WaystoneHandle.Vanilla> makeWideSign(
		PostTile tile,
		SignBlockPart<?> generatorPart,
		Direction facing,
		ServerLevel world,
		BlockPos tilePos,
		Queue<Tuple<BlockPos, WaystoneHandle.Vanilla>> possibleTargets,
		float y
	) {
		var nextTargetOption = fetchNextTarget(possibleTargets);
		if(nextTargetOption.isEmpty()) return Collections.emptySet();
		var target = nextTargetOption.get()._1;
		WaystoneData targetData = nextTargetOption.get()._2;

		Angle rotation = SignBlockPart.pointingAt(tilePos, target._1);
		if(tile.getParts().stream().anyMatch(instance -> !(instance.blockPart instanceof PostBlockPart) && !(instance.blockPart instanceof SignBlockPart<?> s && s.isMarkedForGeneration()) && isNearly(instance.offset.y, y))) {
			possibleTargets.add(target);
			return Collections.emptySet();
		}
		tile.addPart(
			new BlockPartInstance(
				new SmallWideSignBlockPart(
					new AngleProvider.WaystoneTarget(rotation), new NameProvider.WaystoneTarget(targetData.name), shouldFlip(facing, rotation),
					generatorPart.getMainTexture(), generatorPart.getSecondaryTexture(),
					overlayFor(world, tilePos), generatorPart.getColor(), Optional.of(target._2),
					ItemStack.EMPTY, tile.modelType, false, false
				),
				new Vector3(0, y, 0)
			),
			ItemStack.EMPTY,
			PlayerHandle.Invalid,
		false
		);
		return Collections.singleton(target._2);
	}

	private static boolean isNearly(float a, float b) { return Math.abs(a - b) < 1e-5f; }

	private static Collection<WaystoneHandle.Vanilla> makeShortSigns(
		PostTile tile,
		SignBlockPart<?> generatorPart,
		Direction facing,
		ServerLevel world,
		BlockPos tilePos,
		Queue<Tuple<BlockPos, WaystoneHandle.Vanilla>> possibleTargets,
		float y
	) {
		var nextTargetOption = fetchNextTarget(possibleTargets);
		if(nextTargetOption.isEmpty()) return Collections.emptySet();
		var target = nextTargetOption.get()._1;
		WaystoneData targetData = nextTargetOption.get()._2;

		Angle rotation = SignBlockPart.pointingAt(tilePos, target._1);
		boolean shouldFlip = shouldFlip(facing, rotation);
		Optional<Overlay> overlay = overlayFor(world, tilePos);
		List<Consumer<PostTile>> onTileFetched = new ArrayList<>();
		tile.addPart(
			new BlockPartInstance(
				new SmallShortSignBlockPart(
					new AngleProvider.WaystoneTarget(rotation), new NameProvider.WaystoneTarget(targetData.name), shouldFlip,
					generatorPart.getMainTexture(), generatorPart.getSecondaryTexture(),
					overlay, generatorPart.getColor(), Optional.of(target._2),
					ItemStack.EMPTY, tile.modelType, false, false
				),
				new Vector3(0, y, 0)
			),
			ItemStack.EMPTY,
			PlayerHandle.Invalid,
			false
		);

		var secondNextTargetOption = fetchNextTarget(possibleTargets);
		if(secondNextTargetOption.isEmpty()) return Collections.singleton(target._2);
		var secondTarget = secondNextTargetOption.get()._1;

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
			tile.addPart(
				new BlockPartInstance(
					new SmallShortSignBlockPart(
						new AngleProvider.WaystoneTarget(secondRotation), new NameProvider.WaystoneTarget(secondTargetData.name), shouldSecondFlip,
						generatorPart.getMainTexture(), generatorPart.getSecondaryTexture(),
						overlay, generatorPart.getColor(), Optional.of(secondTargetHandle),
						ItemStack.EMPTY, tile.modelType, false, false
					),
					new Vector3(0, y, 0)
				),
				ItemStack.EMPTY,
				PlayerHandle.Invalid,
				false
			);
			break;
		}
		skippedTargets.addAll(possibleTargets);
		possibleTargets.clear();
		possibleTargets.addAll(skippedTargets);
		return secondTarget == null
			? Collections.singleton(target._2)
			: ImmutableList.of(target._2, secondTarget._2);
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
