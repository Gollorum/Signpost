package gollorum.signpost.minecraft.items;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.PostBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;

public class PostItem extends BlockItem {

    public PostItem(PostBlock block, Properties properties) {
        super(block, properties
            .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Signpost.MOD_ID, block.registryName))));
    }

}
