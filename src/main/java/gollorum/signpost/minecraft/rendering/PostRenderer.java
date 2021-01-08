package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.signdata.types.renderers.BlockPartRenderer;
import gollorum.signpost.utils.BlockPartInstance;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;

import java.util.Optional;
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
        matrixStack.push();
        matrixStack.translate(0.5, 0, 0.5);
        matrixStack.translate(randomOffset * random.nextDouble(), randomOffset * random.nextDouble(), randomOffset * random.nextDouble());
        for (BlockPartInstance now: tile.getParts()) {
            matrixStack.push();
            matrixStack.translate(now.offset.x, now.offset.y, now.offset.z);
            BlockPartRenderer.renderDynamic(
                now.blockPart,
                tile,
                renderDispatcher,
                matrixStack,
                buffer,
                combinedLight,
                combinedOverlay,
                random,
                rand
            );
            matrixStack.pop();
        }
        matrixStack.pop();
    }

}
