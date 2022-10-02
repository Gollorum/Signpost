package gollorum.signpost.blockpartdata.types;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.types.renderers.*;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.utils.BlockPart;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BlockPartRenderer<T extends BlockPart<T>> {

    private static final Map<Class<? extends BlockPart>, BlockPartRenderer<? extends BlockPart>> renderers
        = new ConcurrentHashMap<>();

    public static <T extends BlockPart<T>> void register(Class<T> blockPartClass, BlockPartRenderer<T> renderer) {
        renderers.put(blockPartClass, renderer);
    }

    public static <T extends BlockPart<T>> Optional<BlockPartRenderer<T>> getFor(Class<T> blockPartClass) {
        return Optional.ofNullable(renderers.get(blockPartClass))
            .map(renderer -> (BlockPartRenderer<T>) renderer);
    }

    static {
        register(PostBlockPart.class, new PostRenderer());
        register(SmallWideSignBlockPart.class, new WideSignRenderer());
        register(SmallShortSignBlockPart.class, new ShortSignRenderer());
        register(LargeSignBlockPart.class, new LargeSignRenderer());
        register(WaystoneBlockPart.class, new WaystoneRenderer());
    }

    public static <T extends BlockPart<T>> void renderDynamic(
        T part,
        BlockEntity tileEntity,
        BlockEntityRenderDispatcher renderDispatcher,
        PoseStack matrix,
        MultiBufferSource buffer,
        int combinedLights,
        int combinedOverlay,
        Random random,
        long randomSeed
    ) {
        Optional<BlockPartRenderer<T>> renderer = BlockPartRenderer.getFor((Class<T>) part.getClass());
        if(renderer.isPresent()) {
            renderer.get().render(
                part,
                tileEntity,
                renderDispatcher,
                matrix,
                buffer,
                combinedLights,
                combinedOverlay,
                random,
                randomSeed
            );
        } else {
            Signpost.LOGGER.error("Block part renderer was not found for " + part.getClass());
        }
    }

    public static <T extends BlockPart<T>> void renderGuiDynamic(
        T part, PoseStack matrixStack, Point center, Angle yaw, Angle pitch, boolean isFlipped, float scale, Vector3 offset
    ) {
        Optional<BlockPartRenderer<T>> renderer = BlockPartRenderer.getFor((Class<T>) part.getClass());
        if(renderer.isPresent()) {
            renderer.get().renderGui(
                part,
                matrixStack,
                center,
                yaw,
                pitch,
                isFlipped,
                scale,
                offset
            );
        } else {
            Signpost.LOGGER.error("Block part renderer was not found for " + part.getClass());
        }
    }

    public static <T extends BlockPart<T>> void renderGuiDynamic(
        T part, PoseStack matrixStack, Vector3 offset, MultiBufferSource buffer, int combinedLight, int combinedOverlay
    ) {
        Optional<BlockPartRenderer<T>> renderer = BlockPartRenderer.getFor((Class<T>) part.getClass());
        if(renderer.isPresent()) {
            renderer.get().renderGui(
                part,
                matrixStack,
                offset,
                buffer,
                combinedLight,
                combinedOverlay
            );
        } else {
            Signpost.LOGGER.error("Block part renderer was not found for " + part.getClass());
        }
    }

    public abstract void render(
        T part,
        BlockEntity tileEntity,
        BlockEntityRenderDispatcher renderDispatcher,
        PoseStack matrix,
        MultiBufferSource buffer,
        int combinedLights,
        int combinedOverlay,
        Random random,
        long randomSeed
    );

    public abstract void renderGui(
        T part, PoseStack matrixStack, Point center, Angle yaw, Angle pitch, boolean isFlipped, float scale, Vector3 offset
    );

    public abstract void renderGui(
        T part, PoseStack matrixStack, Vector3 offset, MultiBufferSource buffer, int combinedLight, int combinedOverlay
    );

}
