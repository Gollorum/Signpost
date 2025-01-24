package gollorum.signpost.minecraft.worldgen;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.minecraft.config.IConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.LegacySinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class WaystoneJigsawPiece extends LegacySinglePoolElement {

	private static Set<BlockPos> generatedPieces;
	public static void reset() {
		generatedPieces = new HashSet<>();
	}

	public static final MapCodec<WaystoneJigsawPiece> codec = RecordCodecBuilder.mapCodec((codecBuilder) ->
		codecBuilder.group(templateCodec(), processorsCodec(), projectionCodec(), overrideLiquidSettingsCodec()).apply(codecBuilder, WaystoneJigsawPiece::new));

	public WaystoneJigsawPiece(
		ResourceLocation location,
		Holder<StructureProcessorList> structureProcessorListSupplier,
		StructureTemplatePool.Projection placementBehaviour,
        Optional<LiquidSettings> liquidSettings
	) {
		this(Either.left(location), structureProcessorListSupplier, placementBehaviour, liquidSettings);
	}

	public WaystoneJigsawPiece(
		Either<ResourceLocation, StructureTemplate> template,
		Holder<StructureProcessorList> structureProcessorListSupplier,
		StructureTemplatePool.Projection placementBehaviour,
        Optional<LiquidSettings> liquidSettings
	) {
		super(template, structureProcessorListSupplier, placementBehaviour, liquidSettings);
	}

	@Override
	public boolean place(
        StructureTemplateManager templateManager,
        WorldGenLevel seedReader,
        StructureManager structureManager,
        ChunkGenerator chunkGenerator,
        BlockPos pieceLocation,
        BlockPos villageLocation,
        Rotation rotation,
        BoundingBox boundingBox,
        RandomSource random,
        LiquidSettings liquidSettings,
        boolean keepJigsaws
	) {
		if(!IConfig.IServer.getInstance().worldGen().isVillageGenerationEnabled()) return false;
		if(generatedPieces.contains(villageLocation) || VillageWaystone.doesWaystoneExistIn(villageLocation)) return false;

		StructurePlaceSettings placementSettings = this.getSettings(rotation, boundingBox, liquidSettings, keepJigsaws);

		StructureTemplate template = this.template.map(templateManager::getOrCreate, Function.identity());
		if(template.placeInWorld(seedReader, pieceLocation, villageLocation, placementSettings, random, 18)) {
			generatedPieces.add(villageLocation);
			return true;
		} else return false;
	}

	@Override
	public @NotNull StructurePoolElementType<?> getType() {
		return JigsawDeserializers.waystone.get();
	}

	@Override
	public @NotNull String toString() {
		return "SingleSignpostWaystone[" + this.template + "]";
	}

}
