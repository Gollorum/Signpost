package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.blockpartdata.types.BlockPartRenderer;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.mixin.LevelRendererAccessor;
import gollorum.signpost.utils.BlockPartInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.stream.Collectors;

public class PostRenderer implements BlockEntityRenderer<PostTile> {

    private static final double randomOffset = 0.001;

    private final BlockEntityRenderDispatcher renderer;

    public PostRenderer(BlockEntityRendererProvider.Context ctx) {
        renderer = ctx.getBlockEntityRenderDispatcher();
    }

    @Override
    public void render(
        PostTile tile, float partialTicks, PoseStack matrixStack,
        MultiBufferSource buffer, int combinedLight, int combinedOverlay,
        Vec3 whateverThisIs
    ) {
        long randomSeed = tile.hashCode();
        RandomSource random = RandomSource.create(randomSeed);
        SortedSet<BlockDestructionProgress> destructionProgresses = ((LevelRendererAccessor)Minecraft.getInstance().levelRenderer).getDestructionProgress().get(tile.getBlockPos().asLong());
        Set<BlockPartInstance> partsBeingBroken = destructionProgresses == null ? null : destructionProgresses.stream()
            .map(progress -> Optional.ofNullable(tile.getLevel().getEntity(progress.getId()))
                .flatMap(tile::trace)
                .map(res -> res.part))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
        boolean shouldUseOriginalBuffer = partsBeingBroken == null || partsBeingBroken.isEmpty() || partsBeingBroken.stream().anyMatch(i -> i.blockPart() instanceof PostBlockPart);
        RenderingUtil.wrapInMatrixEntry(matrixStack, () -> {
            matrixStack.translate(0.5, 0, 0.5);
            for (BlockPartInstance now: tile.getParts()) {
                RenderingUtil.wrapInMatrixEntry(matrixStack, () -> {
                    matrixStack.translate(
                        now.offset().x() + randomOffset * random.nextDouble(),
                        now.offset().y() + randomOffset * random.nextDouble(),
                        now.offset().z() + randomOffset * random.nextDouble());
                    BlockPartRenderer.renderDynamic(
                        now.blockPart(),
                        tile.getLevel(),
                        tile.getBlockPos(),
                        matrixStack,
                        shouldUseOriginalBuffer || partsBeingBroken.contains(now) ? buffer : Minecraft.getInstance().renderBuffers().bufferSource(),
                        combinedLight,
                        combinedOverlay,
                        t -> RenderType.cutout()
                    );
                });
            }
        });
    }

}
