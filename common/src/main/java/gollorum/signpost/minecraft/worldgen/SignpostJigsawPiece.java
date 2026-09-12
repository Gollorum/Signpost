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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class SignpostJigsawPiece extends LegacySinglePoolElement {

    // Placement runs on the world generation executor, so several villages can be placed at once.
    private static Map<BlockPos, AtomicInteger> signpostCountForVillage;
    public static void reset() {
        signpostCountForVillage = new ConcurrentHashMap<>();
    }
    public static final MapCodec<SignpostJigsawPiece> codec = RecordCodecBuilder.mapCodec((codecBuilder) ->
        codecBuilder.group(templateCodec(), processorsCodec(), projectionCodec(), overrideLiquidSettingsCodec(), isZombieCodec())
            .apply(codecBuilder, SignpostJigsawPiece::new));

    private static RecordCodecBuilder<SignpostJigsawPiece, Boolean> isZombieCodec() {
        return Codec.BOOL.fieldOf("isZombie").forGetter(o -> o.isZombie);
    }

    public final boolean isZombie;

    public SignpostJigsawPiece(
        ResourceLocation location,
        Holder<StructureProcessorList> structureProcessorListSupplier,
        StructureTemplatePool.Projection placementBehaviour,
        Optional<LiquidSettings> liquidSettings,
        boolean isZombie
    ) {
        this(Either.left(location), structureProcessorListSupplier, placementBehaviour, liquidSettings, isZombie);
    }

    public SignpostJigsawPiece(
        Either<ResourceLocation, StructureTemplate> template,
        Holder<StructureProcessorList> structureProcessorListSupplier,
        StructureTemplatePool.Projection placementBehaviour,
        Optional<LiquidSettings> liquidSettings,
        boolean isZombie
    ) {
        super(template, structureProcessorListSupplier, placementBehaviour, liquidSettings);
        this.isZombie = isZombie;
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
        AtomicInteger placed = signpostCountForVillage.computeIfAbsent(villageLocation, k -> new AtomicInteger());
        int max = IConfig.IServer.getInstance().worldGen().maxSignpostsPerVillage();
        // Claim a slot before placing, so that the limit holds even when villages are placed concurrently.
        if(placed.incrementAndGet() > max) {
            placed.decrementAndGet();
            return false;
        }
        StructureTemplate template = this.template.map(templateManager::getOrCreate, Function.identity());
        StructurePlaceSettings placementSettings = this.getSettings(rotation, boundingBox, liquidSettings, keepJigsaws);
        if (template.placeInWorld(seedReader, pieceLocation, villageLocation, placementSettings, random, 18)) {
            return true;
        } else {
            placed.decrementAndGet();
            return false;
        }
    }

    @Override
    public @NotNull StructurePoolElementType<?> getType() {
        return JigsawDeserializers.signpost;
    }

    @Override
    public @NotNull String toString() {
        return "SingleSignpost[" + this.template + "]";
    }

}