package gollorum.signpost.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.gui.utils.TextureResource;
import gollorum.signpost.registry.BlockRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * 1.21.1 has no {@code net.minecraft.client.data.models.ModelProvider} for mods to extend, and the
 * vanilla generators keep their outputs package-private, so block models and blockstates are emitted
 * through NeoForge's own generator - the standard route on this version. Item models live in
 * {@link ItemModels}, because 1.21.1 has no combined block+item model provider either.
 */
public class Models extends BlockStateProvider {

    public Models(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Signpost.MOD_ID, existingFileHelper);
    }

    private static String pathOf(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }

    @Override
    protected void registerStatesAndModels() {
        registerPosts();
        registerWaystones();
        registerGenerator();
    }

    private void registerPosts() {
        PostBlock.all().forEach(block -> {
            // Particle only: a post's geometry is drawn by PostRenderer, not by a block model. The
            // particle texture follows the material, since 2.04 a post block is one of four materials
            // rather than one block per wood type.
            var model = models().getBuilder("block/" + pathOf(block))
                .texture("particle", particleTextureFor(block.materialType));
            simpleBlock(block, model);
        });
    }

    private static ResourceLocation particleTextureFor(PostBlock.MaterialType materialType) {
        return ResourceLocation.withDefaultNamespace("block/" + switch (materialType) {
            case Wood -> "oak_log";
            case Stone -> "stone";
            case Metal -> "iron_block";
            case Mushroom -> "red_mushroom_block";
        });
    }

    private void registerWaystones() {
        var waystoneTexture = TextureResource.waystoneTextureLocation.identifier();
        var waystone = BlockRegistry.WaystoneBlock.get();
        simpleBlock(waystone, models().cubeAll(pathOf(waystone), waystoneTexture));

        // The ModelWaystone block models are hand-authored in common's resources; only the blockstate
        // is generated, rotating that model to face each horizontal direction.
        for (var variant : ModelWaystone.variants) {
            var model = models().getExistingFile(
                ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "block/" + variant.registryName));
            getVariantBuilder(variant.getBlock())
                .forAllStatesExcept(state -> ConfiguredModel.builder()
                .modelFile(model)
                .rotationY(switch (state.getValue(ModelWaystone.Facing)) {
                    case SOUTH -> 0;
                    case WEST -> 90;
                    case NORTH -> 180;
                    case EAST -> 270;
                    default -> throw new IllegalStateException(
                        "Unexpected value: " + state.getValue(ModelWaystone.Facing));
                })
                .build(), ModelWaystone.Waterlogged);
        }
    }

    private void registerGenerator() {
        var generator = BlockRegistry.WaystoneGenerator.get();
        simpleBlock(generator, models().cubeAll(
            pathOf(generator), TextureResource.waystoneTextureLocation.identifier()));
    }
}
