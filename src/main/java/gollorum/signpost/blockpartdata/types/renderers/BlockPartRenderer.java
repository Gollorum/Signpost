package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.types.*;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.utils.BlockPart;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;

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
        register(Post.class, new PostRenderer());
        register(SmallWideSign.class, new WideSignRenderer());
        register(SmallShortSign.class, new ShortSignRenderer());
        register(LargeSign.class, new LargeSignRenderer());
        register(Waystone.class, new WaystoneRenderer());
    }

    public static <T extends BlockPart<T>> void renderDynamic(
        T part,
        TileEntity tileEntity,
        TileEntityRendererDispatcher renderDispatcher,
        MatrixStack matrix,
        IRenderTypeBuffer buffer,
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
        T part, Point center, float yaw, float pitch, float scale, Vector3 offset
    ) {
        Optional<BlockPartRenderer<T>> renderer = BlockPartRenderer.getFor((Class<T>) part.getClass());
        if(renderer.isPresent()) {
            renderer.get().renderGui(
                part,
                center,
                yaw,
                pitch,
                scale,
                offset
            );
        } else {
            Signpost.LOGGER.error("Block part renderer was not found for " + part.getClass());
        }
    }

    public abstract void render(
        T part,
        TileEntity tileEntity,
        TileEntityRendererDispatcher renderDispatcher,
        MatrixStack matrix,
        IRenderTypeBuffer buffer,
        int combinedLights,
        int combinedOverlay,
        Random random,
        long randomSeed
    );

    public abstract void renderGui(
        T part, Point center, float yaw, float pitch, float scale, Vector3 offset
    );

}
