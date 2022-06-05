package gollorum.signpost.minecraft.worldgen;

import com.google.common.collect.Streams;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.BlockPosSerializer;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.ResourceLocationSerializer;
import gollorum.signpost.worldgen.VillageNamesProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class WaystoneJigsawPiece extends SinglePoolElement {

	public static class ChunkEntryKey {
		public final ChunkPos chunkPos;
		public final ResourceLocation dimensionKey;

		public ChunkEntryKey(ChunkPos chunkPos, ResourceLocation dimensionKey) {
			this.chunkPos = chunkPos;
			this.dimensionKey = dimensionKey;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ChunkEntryKey that = (ChunkEntryKey) o;
			return chunkPos.equals(that.chunkPos) && dimensionKey.equals(that.dimensionKey);
		}

		@Override
		public int hashCode() {
			return Objects.hash(chunkPos, dimensionKey);
		}

		public static final Serializer serializer = new Serializer();
		public static class Serializer implements CompoundSerializable<ChunkEntryKey> {

			@Override
			public Class<ChunkEntryKey> getTargetClass() {
				return ChunkEntryKey.class;
			}

			@Override
			public CompoundTag write(ChunkEntryKey key, CompoundTag compound) {
				compound.putInt("x", key.chunkPos.x);
				compound.putInt("z", key.chunkPos.z);
				ResourceLocationSerializer.Instance.write(key.dimensionKey, compound);
				return compound;
			}

			@Override
			public boolean isContainedIn(CompoundTag compound) {
				return compound.contains("x") && compound.contains("z") &&
					ResourceLocationSerializer.Instance.isContainedIn(compound);
			}

			@Override
			public ChunkEntryKey read(CompoundTag compound) {
				return new ChunkEntryKey(
					new ChunkPos(compound.getInt("x"), compound.getInt("z")),
					ResourceLocationSerializer.Instance.read(compound)
				);
			}
		}
	}

	// Key is not the position of the block, it's a reference position.
	// This is usually the village's position.
	private static final Map<BlockPos, WaystoneHandle.Vanilla> generatedWaystones = new HashMap<>();
	private static final Map<ChunkEntryKey, WaystoneHandle.Vanilla> generatedWaystonesByChunk = new HashMap<>();

	public static void reset() {
		generatedWaystones.clear();
		WaystoneDiscoveryEventListener.initialize();
	}

	public static Tag serialize() {
		ListTag ret = new ListTag();
		ret.addAll(Streams.zip(
			generatedWaystones.entrySet().stream(),
			generatedWaystonesByChunk.keySet().stream(),
			(e, c) -> {
				CompoundTag compound = new CompoundTag();
				compound.put("refPos", BlockPosSerializer.INSTANCE.write(e.getKey()));
				compound.put("chunkEntryKey", ChunkEntryKey.serializer.write(c));
				compound.put("waystone", WaystoneHandle.Vanilla.Serializer.write(e.getValue()));
				return compound;
		}).collect(Collectors.toList()));
		return ret;
	}

	public static void deserialize(ListTag nbt) {
		generatedWaystones.clear();
		generatedWaystones.putAll(
			nbt.stream().collect(Collectors.toMap(
				entry -> BlockPosSerializer.INSTANCE.read(((CompoundTag) entry).getCompound("refPos")),
				entry -> WaystoneHandle.Vanilla.Serializer.read(((CompoundTag) entry).getCompound("waystone"))
			)));
		generatedWaystonesByChunk.clear();
		generatedWaystonesByChunk.putAll(
			nbt.stream().collect(Collectors.toMap(
				entry -> ChunkEntryKey.serializer.read(((CompoundTag) entry).getCompound("chunkEntryKey")),
				entry -> WaystoneHandle.Vanilla.Serializer.read(((CompoundTag) entry).getCompound("waystone"))
			)));
	}

	public static final Codec<WaystoneJigsawPiece> codec = RecordCodecBuilder.create((codecBuilder) ->
		codecBuilder.group(templateCodec(), processorsCodec(), projectionCodec()).apply(codecBuilder, WaystoneJigsawPiece::new));

	public WaystoneJigsawPiece(
		ResourceLocation location,
		Supplier<StructureProcessorList> structureProcessorListSupplier,
		StructureTemplatePool.Projection placementBehaviour
	) {
		this(Either.left(location), structureProcessorListSupplier, placementBehaviour);
	}

	public WaystoneJigsawPiece(
		Either<ResourceLocation, StructureTemplate> template,
		Supplier<StructureProcessorList> structureProcessorListSupplier,
		StructureTemplatePool.Projection placementBehaviour
	) {
		super(template, structureProcessorListSupplier, placementBehaviour);
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
		if(generatedWaystones.containsKey(villageLocation)) return false;

		List<ModelWaystone> allowedWaystones = getAllowedWaystones();
		if(allowedWaystones.size() == 0) {
			Signpost.LOGGER.warn("Tried to generate a waystone, but the list of allowed waystones was empty.");
			return false;
		}
		StructurePlaceSettings placementSettings = this.getSettings(rotation, boundingBox, shouldUseJigsawReplacementStructureProcessor);
		Direction facing = placementSettings.getRotation().rotate(Direction.WEST);
		Direction left = placementSettings.getRotation().rotate(Direction.SOUTH);
		BlockPos pos = pieceLocation.relative(facing.getOpposite()).relative(left).above();

		ModelWaystone waystone = getWaystoneType(random, allowedWaystones);
		Optional<String> optionalName = VillageNamesProvider.requestFor(pos, villageLocation, seedReader.getLevel());
		if(!optionalName.isPresent()) {
			Signpost.LOGGER.warn("No name could be generated for waystone at " + pos + ".");
			return false;
		}
		String name = optionalName.get();

		StructureTemplate template = this.template.map(templateManager::getOrCreate, Function.identity());
		if (template.placeInWorld(seedReader, pieceLocation, villageLocation, placementSettings, random, 18)) {
			seedReader.setBlock(pos, waystone.defaultBlockState().setValue(ModelWaystone.Facing, facing.getOpposite()), 18);
			WaystoneLibrary.getInstance().update(
				name,
				locationDataFor(pos, seedReader, facing),
				null,
				false
			);
			registerGenerated(name, villageLocation, seedReader.getLevel(), pos);

			for(StructureTemplate.StructureBlockInfo blockInfo : StructureTemplate.processBlockInfos(
				seedReader, pieceLocation, villageLocation, placementSettings,
				this.getDataMarkers(templateManager, pieceLocation, rotation, false), template
			)) {
				this.handleDataMarker(seedReader, blockInfo, pieceLocation, rotation, random, boundingBox);
			}

			return true;
		} else {
			return false;
		}
	}

	private static WaystoneLocationData locationDataFor(BlockPos pos, WorldGenLevel world, Direction facing) {
		return new WaystoneLocationData(new WorldLocation(pos, world.getLevel()), spawnPosFor(world, pos, facing));
	}

	private static Vector3 spawnPosFor(WorldGenLevel world, BlockPos waystonePos, Direction facing) {
		BlockPos spawnBlockPos = waystonePos.relative(facing, 2);
		int maxOffset = 10;
		int offset = 0;
		while(world.getBlockState(spawnBlockPos).isAir() && offset <= maxOffset) {
			spawnBlockPos = spawnBlockPos.below();
			offset++;
		}
		offset = 0;
		while(!world.getBlockState(spawnBlockPos).isAir() && offset <= maxOffset) {
			spawnBlockPos = spawnBlockPos.above();
			offset++;
		}
		return Vector3.fromBlockPos(spawnBlockPos).add(0.5f, 0, 0.5f);
	}

	private static ModelWaystone getWaystoneType(Random random, List<ModelWaystone> allowedWaystones) {
		return allowedWaystones.get(random.nextInt(allowedWaystones.size()));
	}

	private static void registerGenerated(String name, BlockPos referencePos, ServerLevel world, BlockPos blockPos) {
		WaystoneLibrary.getInstance().getHandleByName(name).ifPresent(handle -> {
			ChunkEntryKey key = new ChunkEntryKey(new ChunkPos(blockPos), world.dimension().location());
			generatedWaystones.put(referencePos, handle);
			generatedWaystonesByChunk.put(key, handle);
			WaystoneLibrary.getInstance().markDirty();
			WaystoneDiscoveryEventListener.registerNew(handle, world, blockPos);
		});
	}

	public static Set<Map.Entry<BlockPos, WaystoneHandle.Vanilla>> getAllEntries() {
		List<BlockPos> toRemove = generatedWaystones.entrySet().stream()
			.filter(e -> WaystoneLibrary.getInstance().getData(e.getValue()).isEmpty())
			.map(Map.Entry::getKey)
			.toList();
		for(BlockPos key : toRemove) generatedWaystones.remove(key);
		return generatedWaystones.entrySet();
	}

	public static Map<ChunkEntryKey, WaystoneHandle.Vanilla> getAllEntriesByChunk() {
		List<ChunkEntryKey> toRemove = generatedWaystonesByChunk.entrySet().stream()
			.filter(e -> WaystoneLibrary.getInstance().getData(e.getValue()).isEmpty())
			.map(Map.Entry::getKey)
			.toList();
		for(ChunkEntryKey key : toRemove) generatedWaystonesByChunk.remove(key);
		return generatedWaystonesByChunk;
	}

	private static List<ModelWaystone> getAllowedWaystones() {
		return ModelWaystone.variants.stream()
			.filter(v -> Config.Server.worldGen.allowedVillageWaystones.get().contains(v.name))
			.map(v -> v.block)
			.collect(Collectors.toList());
	}

	public StructurePoolElementType<?> getType() {
		return JigsawDeserializers.waystone;
	}

	public String toString() {
		return "SingleSignpostWaystone[" + this.template + "]";
	}

}
