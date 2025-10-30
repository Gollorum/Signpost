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
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class PostRenderer implements BlockEntityRenderer<PostTile, PostRenderer.PostRenderState> {

    public static class PostRenderState extends BlockEntityRenderState {
        PostTile tile;
    }

    private static final double randomOffset = 0.001;

    private final MaterialSet materials;

    public PostRenderer(BlockEntityRendererProvider.Context ctx) {
        materials = ctx.materials();
    }

    @Override
    public PostRenderState createRenderState() {
        return new PostRenderState();
    }

    @Override
    public void extractRenderState(PostTile blockEntity, PostRenderState renderState, float partialTick, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        renderState.tile = blockEntity;
    }

    @Override
    public void submit(PostRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        long randomSeed = renderState.tile.hashCode();
        RandomSource random = RandomSource.create(randomSeed);
        SortedSet<BlockDestructionProgress> destructionProgresses = ((LevelRendererAccessor)Minecraft.getInstance().levelRenderer).getDestructionProgress().get(renderState.blockPos.asLong());
        Set<BlockPartInstance> partsBeingBroken = destructionProgresses == null ? null : destructionProgresses.stream()
            .map(progress -> Optional.ofNullable(renderState.tile.getLevel().getEntity(progress.getId()))
                .flatMap(renderState.tile::trace)
                .map(res -> res.part))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
        boolean shouldUseOriginalBuffer = partsBeingBroken == null || partsBeingBroken.isEmpty() || partsBeingBroken.stream().anyMatch(i -> i.blockPart() instanceof PostBlockPart);
        RenderingUtil.wrapInMatrixEntry(poseStack, () -> {
            poseStack.translate(0.5, 0, 0.5);
            for (BlockPartInstance now: renderState.tile.getParts()) {
                RenderingUtil.wrapInMatrixEntry(poseStack, () -> {
                    poseStack.translate(
                        now.offset().x() + randomOffset * random.nextDouble(),
                        now.offset().y() + randomOffset * random.nextDouble(),
                        now.offset().z() + randomOffset * random.nextDouble());
                    BlockPartRenderer.renderDynamic(
                        now.blockPart(),
                        renderState.tile.getLevel(),
                        renderState.blockPos,
                        poseStack,
                        submitNodeCollector,
                        materials,
                        renderState.lightCoords,
                        OverlayTexture.NO_OVERLAY,
                        t -> RenderType.cutout(),
                        shouldUseOriginalBuffer || partsBeingBroken.contains(now) ? renderState.breakProgress : null
                    );
                });
            }
        });
    }

}
