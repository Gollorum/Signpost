package gollorum.signpost.minecraft.worldgen;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.Config;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.BlockPosSerializer;
import gollorum.signpost.worldgen.VillageNamesProvider;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.ISeedReader;
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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class WaystoneJigsawPiece extends SingleJigsawPiece {

	public static final Map<BlockPos, WaystoneHandle> generatedWaystones = new HashMap<>();

	public static void reset() {
		generatedWaystones.clear();
	}

	public static INBT serialize() {
		ListNBT ret = new ListNBT();
		ret.addAll(generatedWaystones.entrySet().stream().map(e -> {
			CompoundNBT compound = new CompoundNBT();
			BlockPosSerializer.INSTANCE.writeTo(e.getKey(), compound, "refPos");
			WaystoneHandle.SERIALIZER.writeTo(e.getValue(), compound, "waystone");
			return compound;
		}).collect(Collectors.toList()));
		return ret;
	}

	public static void deserialize(ListNBT nbt) {
		generatedWaystones.clear();
		generatedWaystones.putAll(
			nbt.stream().collect(Collectors.toMap(
				entry -> BlockPosSerializer.INSTANCE.read((CompoundNBT) entry, "refPos"),
				entry -> WaystoneHandle.SERIALIZER.read((CompoundNBT) entry, "waystone")
			)));
	}

	public static final Codec<WaystoneJigsawPiece> codec = RecordCodecBuilder.create((codecBuilder) ->
		codecBuilder.group(func_236846_c_(), func_236844_b_(), func_236848_d_()).apply(codecBuilder, WaystoneJigsawPiece::new));

	public WaystoneJigsawPiece(
		ResourceLocation location,
		Supplier<StructureProcessorList> structureProcessorListSupplier,
		JigsawPattern.PlacementBehaviour placementBehaviour
	) {
		this(Either.left(location), structureProcessorListSupplier, placementBehaviour);
	}

	public WaystoneJigsawPiece(
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

		if(generatedWaystones.containsKey(villageLocation)) {
			return false;
		}
		List<ModelWaystone> allowedWaystones = getAllowedWaystones();
		if(allowedWaystones.size() == 0) {
			Signpost.LOGGER.warn("Tried to generate a waystone, but the list of allowed waystones was empty.");
			return false;
		}
		PlacementSettings placementSettings = this.func_230379_a_(rotation, boundingBox, shouldUseJigsawReplacementStructureProcessor);
		Direction facing = placementSettings.getRotation().rotate(Direction.WEST);
		Direction left = placementSettings.getRotation().rotate(Direction.SOUTH);
		BlockPos pos = pieceLocation.offset(facing.getOpposite()).offset(left).up();

		ModelWaystone waystone = getWaystoneType(random, allowedWaystones);
		Optional<String> optionalName = VillageNamesProvider.requestFor(pos, villageLocation, seedReader.getWorld(), random);
		if(!optionalName.isPresent()) {
			Signpost.LOGGER.warn("No name could be generated for waystone at " + pos + ".");
			return false;
		}
		String name = optionalName.get();

		Template template = this.field_236839_c_.map(templateManager::getTemplateDefaulted, Function.identity());
		if (template.func_237146_a_(seedReader, pieceLocation, villageLocation, placementSettings, random, 18)) {
			seedReader.setBlockState(pos, waystone.getDefaultState().with(ModelWaystone.Facing, facing.getOpposite()), 18);
			WaystoneLibrary.getInstance().update(
				name,
				locationDataFor(pos, seedReader, facing),
				PlayerHandle.Invalid,
				false
			);
			registerGenerated(name, villageLocation);

			for(Template.BlockInfo blockInfo : Template.processBlockInfos(
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

	private static WaystoneLocationData locationDataFor(BlockPos pos, ISeedReader world, Direction facing) {
		return new WaystoneLocationData(new WorldLocation(pos, world.getWorld()), spawnPosFor(world, pos, facing));
	}

	private static Vector3 spawnPosFor(ISeedReader world, BlockPos waystonePos, Direction facing) {
		BlockPos spawnBlockPos = waystonePos.offset(facing, 2);
		int maxOffset = 10;
		int offset = 0;
		while(world.getBlockState(spawnBlockPos).isAir() && offset <= maxOffset) {
			spawnBlockPos = spawnBlockPos.down();
			offset++;
		}
		offset = 0;
		while(!world.getBlockState(spawnBlockPos).isAir() && offset <= maxOffset) {
			spawnBlockPos = spawnBlockPos.up();
			offset++;
		}
		return Vector3.fromBlockPos(spawnBlockPos).add(0.5f, 0, 0.5f);
	}

	private static ModelWaystone getWaystoneType(Random random, List<ModelWaystone> allowedWaystones) {
		return allowedWaystones.get(random.nextInt(allowedWaystones.size()));
	}

	private static void registerGenerated(String name, BlockPos referencePos) {
		WaystoneLibrary.getInstance().getHandleByName(name).ifPresent(handle -> generatedWaystones.put(referencePos, handle));
	}

	public static Set<Map.Entry<BlockPos, WaystoneHandle>> getAllEntries() {
		return generatedWaystones.entrySet();
	}

	private static List<ModelWaystone> getAllowedWaystones() {
		return ModelWaystone.variants.stream()
			.filter(v -> Config.Server.allowedVillageWaystones.get().contains(v.name))
			.map(v -> v.block)
			.collect(Collectors.toList());
	}

	public IJigsawDeserializer<?> getType() {
		return JigsawDeserializers.waystone;
	}

	public String toString() {
		return "SingleSignpostWaystone[" + this.field_236839_c_ + "]";
	}

}
