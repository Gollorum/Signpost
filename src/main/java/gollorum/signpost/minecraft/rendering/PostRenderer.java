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
        matrixStack.pushPose();
        matrixStack.translate(0.5, 0, 0.5);
        matrixStack.translate(randomOffset * random.nextDouble(), randomOffset * random.nextDouble(), randomOffset * random.nextDouble());
        for (BlockPartInstance now: tile.getParts()) {
            matrixStack.pushPose();
            matrixStack.translate(now.offset.x, now.offset.y, now.offset.z);
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
            matrixStack.popPose();
        }
        matrixStack.popPose();
    }

}
