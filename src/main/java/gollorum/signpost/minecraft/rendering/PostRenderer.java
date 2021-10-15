package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.blockpartdata.types.renderers.BlockPartRenderer;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.utils.BlockPartInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DestroyBlockProgress;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import java.util.*;
import java.util.stream.Collectors;

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
        SortedSet<DestroyBlockProgress> destructionProgresses = Minecraft.getInstance().levelRenderer.destructionProgress.get(tile.getBlockPos().asLong());
        Set<BlockPartInstance> partsBeingBroken = destructionProgresses == null ? null : destructionProgresses.stream()
            .map(progress -> Optional.ofNullable(tile.getLevel().getEntity(progress.id))
                .flatMap(tile::trace)
                .map(res -> res.part))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
        boolean shouldUseOriginalBuffer = partsBeingBroken == null || partsBeingBroken.isEmpty() || partsBeingBroken.stream().anyMatch(i -> i.blockPart instanceof PostBlockPart);
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
                        shouldUseOriginalBuffer || partsBeingBroken.contains(now) ? buffer : Minecraft.getInstance().renderBuffers().bufferSource(),
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
