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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

public class SignpostJigsawPiece extends LegacySinglePoolElement {

    private static Map<BlockPos, Integer> signpostCountForVillage;
    public static void reset() {
        signpostCountForVillage = new HashMap<>();
    }
    public static final Codec<SignpostJigsawPiece> codec = RecordCodecBuilder.create((codecBuilder) ->
        codecBuilder.group(templateCodec(), processorsCodec(), projectionCodec(), isZombieCodec()).apply(codecBuilder, SignpostJigsawPiece::new));

    private static RecordCodecBuilder<SignpostJigsawPiece, Boolean> isZombieCodec() {
        return Codec.BOOL.fieldOf("isZombie").forGetter(o -> o.isZombie);
    }

    public final boolean isZombie;

    public SignpostJigsawPiece(
        ResourceLocation location,
        Holder<StructureProcessorList> structureProcessorListSupplier,
        StructureTemplatePool.Projection placementBehaviour,
        boolean isZombie
    ) {
        this(Either.left(location), structureProcessorListSupplier, placementBehaviour, isZombie);
    }

    public SignpostJigsawPiece(
        Either<ResourceLocation, StructureTemplate> template,
        Holder<StructureProcessorList> structureProcessorListSupplier,
        StructureTemplatePool.Projection placementBehaviour,
        boolean isZombie
    ) {
        super(template, structureProcessorListSupplier, placementBehaviour);
        this.isZombie = isZombie;
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
        if(signpostCountForVillage.getOrDefault(villageLocation, 0) >= Config.Server.worldGen.maxSignpostsPerVillage())
            return false;
        StructureTemplate template = this.template.map(templateManager::getOrCreate, Function.identity());
        StructurePlaceSettings placementSettings = this.getSettings(rotation, boundingBox, shouldUseJigsawReplacementStructureProcessor);
        if (template.placeInWorld(seedReader, pieceLocation, villageLocation, placementSettings, random, 18)) {
            signpostCountForVillage.put(villageLocation, signpostCountForVillage.getOrDefault(villageLocation, 0) + 1);
            return true;
        } else {
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