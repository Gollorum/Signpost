package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.blockpartdata.types.renderers.BlockPartRenderer;
import gollorum.signpost.utils.BlockPartInstance;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;

import java.util.Collection;
import java.util.Random;

public class PostRenderer extends TileEntityRenderer<PostTile> {

    private static final double randomOffset = 0.001;

    public PostRenderer(TileEntityRendererDispatcher rendererDispatcherIn) {
        super(rendererDispatcherIn);
    }

    @Override
    public void render(
        PostTile tile, float partialTicks, MatrixStack matrixStack,
        IRenderTypeBuffer buffer, int combinedLight, int combinedOverlay
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
