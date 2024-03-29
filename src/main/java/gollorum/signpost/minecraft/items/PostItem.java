package gollorum.signpost.minecraft.items;

import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.rendering.PostItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class PostItem extends BlockItem {

    public PostItem(PostBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return PostItemRenderer.getInstance();
            }
        });
    }
}
