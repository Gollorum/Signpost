package gollorum.signpost;

import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.rendering.PostRenderer;
import gollorum.signpost.networking.FabricPacketHandler;
import gollorum.signpost.utils.Delay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

@Environment(EnvType.CLIENT)
public class SignpostFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(PostTile.getBlockEntityType(), PostRenderer::new);
        FabricPacketHandler.initialize(true);
        Delay.INSTANCE.registerClient();
    }
}
