package gollorum.signpost.data.blocks;

import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.rendering.PostItemRenderer;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;

public class PostModel {

    public static void register(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        for (var variant : PostBlock.AllVariants) {
            blockModels.createParticleOnlyBlock(variant.getBlock(), switch (variant.type.name) {
                case "acacia" -> Blocks.ACACIA_LOG;
                case "birch" -> Blocks.BIRCH_LOG;
                case "iron" -> Blocks.IRON_BLOCK;
                case "jungle" -> Blocks.JUNGLE_LOG;
                case "oak" -> Blocks.OAK_LOG;
                case "darkoak" -> Blocks.DARK_OAK_LOG;
                case "spruce" -> Blocks.SPRUCE_LOG;
                case "mangrove" -> Blocks.MANGROVE_LOG;
                case "bamboo" -> Blocks.BAMBOO_BLOCK;
                case "cherry" -> Blocks.CHERRY_LOG;
                case "stone" -> Blocks.STONE;
                case "red_mushroom" -> Blocks.RED_MUSHROOM_BLOCK;
                case "brown_mushroom" -> Blocks.BROWN_MUSHROOM_BLOCK;
                case "warped" -> Blocks.WARPED_STEM;
                case "crimson" -> Blocks.CRIMSON_STEM;
                case "sandstone" -> Blocks.SANDSTONE;
                default -> throw new IllegalStateException("Unexpected value: " + variant.type.name);
            });
            itemModels.itemModelOutput.accept(
                variant.getBlock().asItem(),
                ItemModelUtils.specialModel(
                    Identifier.withDefaultNamespace("block/cube_all"),
                    new PostItemRenderer.Unbaked(variant.type)
                )
            );
        }
    }
}
