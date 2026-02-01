package gollorum.signpost.data.blocks;

import com.mojang.math.Quadrant;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.gui.utils.TextureResource;
import gollorum.signpost.registry.BlockRegistry;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

public class WaystoneModel {

    public static void register(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        var waystoneTexture = TextureResource.waystoneTextureLocation.identifier();
        blockModels.createTrivialBlock(
            BlockRegistry.WaystoneBlock.get(),
            TexturedModel.createDefault(
                block -> new TextureMapping().put(TextureSlot.ALL, waystoneTexture),
                ModelTemplates.CUBE_ALL
            ));

        // Only generate blockstate files for ModelWaystone variants, no block models
        for (var variant : ModelWaystone.variants) {
            var model = BlockModelGenerators.plainModel(
                Identifier.fromNamespaceAndPath(
                    Signpost.MOD_ID,
                    "block/" + variant.registryName));
            var foo = MultiVariantGenerator.dispatch(variant.getBlock())
                .with(PropertyDispatch.initial(ModelWaystone.Facing)
                    .generate(dir -> switch (dir) {
                        case SOUTH -> BlockModelGenerators.variant(model);
                        case WEST -> BlockModelGenerators.variant(model.withYRot(Quadrant.R90));
                        case NORTH -> BlockModelGenerators.variant(model.withYRot(Quadrant.R180));
                        case EAST -> BlockModelGenerators.variant(model.withYRot(Quadrant.R270));
                        default -> throw new IllegalStateException("Unexpected value: " + dir);
                    }));
            var blockStateDefinition = MultiPartGenerator.multiPart(variant.getBlock())
                .with(
                    BlockModelGenerators.condition().term(ModelWaystone.Facing, Direction.SOUTH).build(),
                    BlockModelGenerators.variant(model)
                ).with(
                    BlockModelGenerators.condition().term(ModelWaystone.Facing, Direction.WEST).build(),
                    BlockModelGenerators.variant(model.withYRot(Quadrant.R90))
                ).with(
                    BlockModelGenerators.condition().term(ModelWaystone.Facing, Direction.NORTH).build(),
                    BlockModelGenerators.variant(model.withYRot(Quadrant.R180))
                ).with(
                    BlockModelGenerators.condition().term(ModelWaystone.Facing, Direction.EAST).build(),
                    BlockModelGenerators.variant(model.withYRot(Quadrant.R270))
                );
            blockModels.blockStateOutput.accept(foo);
        }
    }

}
