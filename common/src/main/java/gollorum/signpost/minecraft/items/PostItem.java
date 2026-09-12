package gollorum.signpost.minecraft.items;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.PostBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;

public class PostItem extends BlockItem {

    public PostItem(PostBlock block, Properties properties) {
        super(block, properties);
    }

}
