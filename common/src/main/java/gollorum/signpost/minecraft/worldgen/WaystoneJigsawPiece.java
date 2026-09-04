package gollorum.signpost.minecraft.worldgen;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.minecraft.config.IConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
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

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class WaystoneJigsawPiece extends LegacySinglePoolElement {

	// Placement runs on the world generation executor, so several villages can be placed at once.
	private static Set<BlockPos> generatedPieces;
	public static void reset() {
		generatedPieces = ConcurrentHashMap.newKeySet();
	}

	public static final MapCodec<WaystoneJigsawPiece> codec = RecordCodecBuilder.mapCodec((codecBuilder) ->
		codecBuilder.group(templateCodec(), processorsCodec(), projectionCodec(), overrideLiquidSettingsCodec()).apply(codecBuilder, WaystoneJigsawPiece::new));

	public WaystoneJigsawPiece(
		Identifier location,
		Holder<StructureProcessorList> structureProcessorListSupplier,
		StructureTemplatePool.Projection placementBehaviour,
        Optional<LiquidSettings> liquidSettings
	) {
		this(Either.left(location), structureProcessorListSupplier, placementBehaviour, liquidSettings);
	}

	public WaystoneJigsawPiece(
		Either<Identifier, StructureTemplate> template,
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
		if(VillageWaystone.getInstance().doesWaystoneExistIn(villageLocation)) return false;
		// Claim the village before placing, so that two threads cannot both decide to place the first waystone.
		if(!generatedPieces.add(villageLocation)) return false;

		StructurePlaceSettings placementSettings = this.getSettings(rotation, boundingBox, liquidSettings, keepJigsaws);

		StructureTemplate template = this.template.map(templateManager::getOrCreate, Function.identity());
		if(template.placeInWorld(seedReader, pieceLocation, villageLocation, placementSettings, random, 18)) {
			return true;
		} else {
			generatedPieces.remove(villageLocation);
			return false;
		}
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
