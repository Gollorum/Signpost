package gollorum.signpost.minecraft.worldgen;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.minecraft.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.LegacySinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

public class WaystoneJigsawPiece extends LegacySinglePoolElement {

	private static Set<BlockPos> generatedPieces;
	public static void reset() {
		generatedPieces = new HashSet<>();
	}

	public static final Codec<WaystoneJigsawPiece> codec = RecordCodecBuilder.create((codecBuilder) ->
		codecBuilder.group(templateCodec(), processorsCodec(), projectionCodec()).apply(codecBuilder, WaystoneJigsawPiece::new));

	public WaystoneJigsawPiece(
		ResourceLocation location,
		Holder<StructureProcessorList> structureProcessorListSupplier,
		StructureTemplatePool.Projection placementBehaviour
	) {
		this(Either.left(location), structureProcessorListSupplier, placementBehaviour);
	}

	public WaystoneJigsawPiece(
		Either<ResourceLocation, StructureTemplate> template,
		Holder<StructureProcessorList> structureProcessorListSupplier,
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
		if(!Config.Server.worldGen.isVillageGenerationEnabled()) return false;
		if(generatedPieces.contains(villageLocation) || VillageWaystone.doesWaystoneExistIn(villageLocation)) return false;

		StructurePlaceSettings placementSettings = this.getSettings(rotation, boundingBox, shouldUseJigsawReplacementStructureProcessor);

		StructureTemplate template = this.template.map(templateManager::getOrCreate, Function.identity());
		if(template.placeInWorld(seedReader, pieceLocation, villageLocation, placementSettings, random, 18)) {
			generatedPieces.add(villageLocation);
			return true;
		} else return false;
	}

	@Override
	public @NotNull StructurePoolElementType<?> getType() {
		return JigsawDeserializers.waystone;
	}

	@Override
	public @NotNull String toString() {
		return "SingleSignpostWaystone[" + this.template + "]";
	}

}
