package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.vertex.*;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.models.modelGeneration.QuadModel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.*;

import java.lang.Math;
import java.util.function.Function;

public class RenderingUtil {

    /**
     * 1.21.1 renders block entities immediately into a {@link MultiBufferSource} - there is no
     * {@code SubmitNodeCollector}, no {@code MaterialSet} and no separate crumbling overlay pass.
     * The breaking overlay is handled for us: {@code BlockEntityRenderDispatcher} hands the renderer
     * a buffer source that is already wrapped in the destroy-progress decal generator, so writing to
     * the buffer we were given is all that is needed.
     */
    public static void render(
        PoseStack blockToView,
        TexturedModel model,
        MultiBufferSource buffer,
        int combinedLights,
        int combinedOverlay,
        Function<ResourceLocation, RenderType> renderTypeFactory
    ) {
        render(
            blockToView.last(),
            model.model(),
            model.texture().buffer(buffer, renderTypeFactory),
            combinedLights,
            combinedOverlay,
            model.tint(),
            0f
        );
    }

    // copied from ModelPart.render, sort of
    public static void render(
        PoseStack.Pose pose,
        QuadModel model,
        VertexConsumer buffer,
        int combinedLights,
        int combinedOverlay,
        int color,
        float offset
    ) {
        var matrix = pose.pose();
        var bufferVector = new Vector3f();

        if ((color & 0xff000000) == 0)
            color |= 0xff000000; // ensure alpha is maxed

        for (var quad : model.quads()) {
            pose.transformNormal(quad.normal(), bufferVector);
            var normalX = bufferVector.x;
            var normalY = bufferVector.y;
            var normalZ = bufferVector.z;

            for (var vertex : quad.vertices()) {
                matrix.transformPosition(vertex.pos(), bufferVector);
                buffer.addVertex(
                    bufferVector.x() + offset * normalX,
                    bufferVector.y() + offset * normalY,
                    bufferVector.z() + offset * normalZ,
                    color,
                    vertex.u(), vertex.v(),
                    combinedOverlay, combinedLights,
                    normalX, normalY, normalZ);
            }
        }
    }

    public static void drawString(GuiGraphics graphics, Font fontRenderer, String text, Point point, Rect.XAlignment xAlignment, Rect.YAlignment yAlignment, int color, int maxWidth, boolean dropShadow) {
        int textWidth = fontRenderer.width(text);
        float scale = Math.min(1f, maxWidth / (float) textWidth);
        graphics.pose().pushPose();
        graphics.pose().translate(
            Rect.xCoordinateFor(point.x, maxWidth, xAlignment) + maxWidth * 0.5f,
            Rect.yCoordinateFor(point.y, fontRenderer.lineHeight, yAlignment) + fontRenderer.lineHeight * 0.5f,
            0f
        );
        if(scale < 1) graphics.pose().scale(scale, scale, 1f);
        graphics.drawString(
            fontRenderer,
            text,
            (maxWidth - Math.min(maxWidth, textWidth)) / 2,
            -fontRenderer.lineHeight / 2,
            color,
            dropShadow
        );
        graphics.pose().popPose();
    }

    public static void wrapInMatrixEntry(PoseStack matrixStack, Runnable thenDo) {
        matrixStack.pushPose();
        thenDo.run();
        matrixStack.popPose();
    }

}
