package gollorum.signpost.blockpartdata.types;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.types.renderers.*;
import gollorum.signpost.utils.BlockPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public abstract class BlockPartRenderer<T extends BlockPart<T>> {

    private static final Map<Class<?>, BlockPartRenderer<?>> renderers
        = new ConcurrentHashMap<>();

    public static <T extends BlockPart<T>> void register(Class<T> blockPartClass, BlockPartRenderer<T> renderer) {
        renderers.put(blockPartClass, renderer);
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockPart<T>> Optional<BlockPartRenderer<T>> getFor(Class<T> blockPartClass) {
        var renderer = renderers.get(blockPartClass);
        return renderer == null
            ? Optional.empty()
            : Optional.of((BlockPartRenderer<T>) renderer);
    }

    static {
        register(PostBlockPart.class, new PostRenderer());
        register(SmallWideSignBlockPart.class, new WideSignRenderer());
        register(SmallShortSignBlockPart.class, new ShortSignRenderer());
        register(LargeSignBlockPart.class, new LargeSignRenderer());
        register(WaystoneBlockPart.class, new WaystoneRenderer());
    }

    @SuppressWarnings("unchecked")
    public static <T extends BlockPart<T>> void renderDynamic(
        T part,
        Level level,
        BlockPos pos,
        PoseStack blockToView,
        SubmitNodeCollector nodeCollector,
        MaterialSet materials,
        int combinedLights,
        int combinedOverlay,
        Function<Identifier, RenderType> renderTypeFactory,
        ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        Optional<BlockPartRenderer<T>> renderer = BlockPartRenderer.getFor((Class<T>) part.getClass());
        if(renderer.isPresent()) {
            renderer.get().render(
                part,
                level,
                pos,
                blockToView,
                nodeCollector,
                materials,
                combinedLights,
                combinedOverlay,
                renderTypeFactory,
                crumblingOverlay
            );
        } else {
            Signpost.LOGGER.error("Block part renderer was not found for " + part.getClass());
        }
    }

    public abstract void render(
        T part,
        Level level,
        BlockPos blockPos,
        PoseStack blockToView,
        SubmitNodeCollector nodeCollector,
        MaterialSet materials, int combinedLights,
        int combinedOverlay,
        Function<Identifier, RenderType> renderTypeFactory,
        ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    );

}
