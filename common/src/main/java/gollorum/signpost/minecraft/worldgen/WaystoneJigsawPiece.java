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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class WaystoneJigsawPiece extends LegacySinglePoolElement {

	/**
	 * Which waystone piece has claimed each village, keyed by the village's reference position.
	 *
	 * <p>The value matters: {@code place} is called <em>once per chunk the piece overlaps</em> -
	 * {@code StructureStart.placeInChunk} walks every piece intersecting the chunk being generated and
	 * hands it a bounding box clipped to that chunk - so a piece straddling a chunk border is asked to
	 * place two or three times, each time writing only its slice. A guard that fires once per village
	 * therefore lets the first slice through and silently drops the rest, leaving a stump of whichever
	 * chunk generated first. Comparing the claiming piece's position lets the same piece back in while
	 * still turning away a second waystone.
	 *
	 * <p>Placement runs on the world generation executor, so several villages can be placed at once.
	 */
	private static Map<BlockPos, BlockPos> claimingPieceByVillage;
	public static void reset() {
		claimingPieceByVillage = new ConcurrentHashMap<>();
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

		// Claim the village before placing, so that two threads cannot both place a waystone. A piece
		// that already holds the claim is let through: that is this same piece coming back for another
		// of its chunks. There is no check against VillageWaystone here on purpose - the waystone
		// registers itself as soon as its first slice is placed, so asking whether the village already
		// has one would refuse this piece its remaining chunks, and would keep refusing them after a
		// restart, where the claim map is empty but the registration has been saved.
		BlockPos claimant = claimingPieceByVillage.putIfAbsent(villageLocation, pieceLocation);
		boolean isNewClaim = claimant == null;
		if(!isNewClaim && !claimant.equals(pieceLocation)) return false;

		StructurePlaceSettings placementSettings = this.getSettings(rotation, boundingBox, liquidSettings, keepJigsaws);

		StructureTemplate template = this.template.map(templateManager::getOrCreate, Function.identity());
		if(template.placeInWorld(seedReader, pieceLocation, villageLocation, placementSettings, random, 18)) {
			return true;
		} else {
			// Only give the claim back if this call is the one that took it; a later chunk failing must
			// not hand the village to a different piece when earlier slices are already in the ground.
			if(isNewClaim) claimingPieceByVillage.remove(villageLocation, pieceLocation);
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
