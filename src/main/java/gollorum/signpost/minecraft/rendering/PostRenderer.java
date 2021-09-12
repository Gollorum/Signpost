package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.blockpartdata.types.renderers.BlockPartRenderer;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.utils.BlockPartInstance;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import java.util.Random;

public class PostRenderer implements BlockEntityRenderer<PostTile> {

    private static final double randomOffset = 0.001;

    private final BlockEntityRenderDispatcher renderer;

    public PostRenderer(BlockEntityRendererProvider.Context ctx) {
        renderer = ctx.getBlockEntityRenderDispatcher();
    }

    @Override
    public void render(
        PostTile tile, float partialTicks, PoseStack matrixStack,
        MultiBufferSource buffer, int combinedLight, int combinedOverlay
    ) {
        Random random = new Random();
        long rand = tile.hashCode();
        random.setSeed(rand);
        RenderingUtil.wrapInMatrixEntry(matrixStack, () -> {
            matrixStack.translate(0.5, 0, 0.5);
            for (BlockPartInstance now: tile.getParts()) {
                RenderingUtil.wrapInMatrixEntry(matrixStack, () -> {
                    matrixStack.translate(now.offset.x + randomOffset * random.nextDouble(), now.offset.y + randomOffset * random.nextDouble(), now.offset.z + randomOffset * random.nextDouble());
                    BlockPartRenderer.renderDynamic(
                        now.blockPart,
                        tile,
                        renderer,
                        matrixStack,
                        buffer,
                        combinedLight,
                        combinedOverlay,
                        random,
                        rand
                    );
                });
            }
        });
    }

}
