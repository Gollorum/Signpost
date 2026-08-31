package gollorum.signpost.data.blocks;

import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.rendering.PostItemRenderer;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;

public class PostModel {

    public static void register(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // The legacy blocks are included: they are still registered, and a registered block without a
        // blockstate definition is an error at resource load time even though nothing ever renders it.
        PostBlock.allIncludingLegacy().forEach(block -> {
            blockModels.createParticleOnlyBlock(block, switch (block.materialType) {
                case PostBlock.MaterialType.Wood -> Blocks.OAK_LOG;
                case PostBlock.MaterialType.Stone -> Blocks.STONE;
                case PostBlock.MaterialType.Mushroom -> Blocks.RED_MUSHROOM_BLOCK;
                case PostBlock.MaterialType.Metal -> Blocks.IRON_BLOCK;
            });
            itemModels.itemModelOutput.accept(
                block.asItem(),
                ItemModelUtils.specialModel(
                    Identifier.withDefaultNamespace("block/cube_all"),
                    new PostItemRenderer.Unbaked(block.materialType)
                )
            );
        });
    }

    private static <T> HolderSet.Named<T> fakeNamedHolder(TagKey<T> tagKey) {
        return HolderSet.emptyNamed(new HolderOwner<>() {}, tagKey);
    }

}
