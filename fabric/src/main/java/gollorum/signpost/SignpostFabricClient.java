package gollorum.signpost;

import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.rendering.PostItemRenderer;
import gollorum.signpost.minecraft.rendering.PostRenderer;
import gollorum.signpost.networking.FabricPacketHandler;
import gollorum.signpost.registry.ItemRegistry;
import gollorum.signpost.utils.Delay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

@Environment(EnvType.CLIENT)
public class SignpostFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(PostTile.getBlockEntityType(), PostRenderer::new);
        // The post items' models are "builtin/entity", which draws nothing on its own - PostItemRenderer
        // has to be bound to each of them. 1.21.1 has no `special` item model to point at it.
        for(var item : ItemRegistry.POSTS_ITEMS)
            BuiltinItemRendererRegistry.INSTANCE.register(item,
                (stack, context, poseStack, buffer, light, overlay) ->
                    PostItemRenderer.getInstance().renderByItem(stack, context, poseStack, buffer, light, overlay));
        FabricPacketHandler.initialize(true);
        Delay.INSTANCE.registerClient();
    }
}
