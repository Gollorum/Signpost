package gollorum.signpost.block;

import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.items.PostItem;
import gollorum.signpost.minecraft.rendering.PostItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * The post items' models are {@code builtin/entity}, which draws nothing by itself - 1.21.1 has no
 * {@code special} item model to name a renderer, so {@link PostItemRenderer} has to be bound to the
 * item. Forge has no {@code RegisterClientExtensionsEvent} (that is NeoForge's), so the binding goes
 * through {@code Item#initializeClient}, which means the post item needs a Forge-side subclass.
 *
 * <p>{@code initializeClient} is only ever called on a physical client, so the client-only types
 * referenced here are never loaded on a dedicated server.
 */
public class PostItemImpl extends PostItem {

    public PostItemImpl(PostBlock block, Properties properties) {
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
