package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.vertex.*;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.models.modelGeneration.QuadModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.Identifier;
import org.joml.*;

import java.lang.Math;
import java.util.function.Function;

public class RenderingUtil {

    public static void render(
        PoseStack blockToView,
        TexturedModel model,
        SubmitNodeCollector nodeCollector,
        MaterialSet materials,
        int combinedLights,
        int combinedOverlay,
        Function<Identifier, RenderType> renderTypeFactory,
        ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        var renderType = model.texture().renderType(renderTypeFactory);
        SubmitNodeCollector.CustomGeometryRenderer doRender = (pose, vertexConsumer) -> {
            render(
                pose,
                model.model(),
                materials.get(model.texture()).wrap(vertexConsumer),
                combinedLights,
                combinedOverlay,
                model.tint(),
                0f
            );

            if (crumblingOverlay != null && renderType.affectsCrumbling()) {
                var crumblingTenderType = ModelBakery.DESTROY_TYPES.get(crumblingOverlay.progress());

                    VertexConsumer vertexconsumer2 = new SheetedDecalTextureGenerator(
                        Minecraft.getInstance().renderBuffers().crumblingBufferSource().getBuffer(crumblingTenderType),
                        crumblingOverlay.cameraPose(),
                        1f
                    );
                    render(
                        pose,
                        model.model(),
                        materials.get(model.texture()).wrap(vertexconsumer2),
                        combinedLights,
                        combinedOverlay,
                        model.tint(),
                        0.001f
                    );
            }
        };
        if (nodeCollector instanceof GuiFakeCollector gfc)
            gfc.submitCustomGeometry(blockToView, renderType, doRender, model.texture().atlasLocation());
        else nodeCollector.submitCustomGeometry(blockToView, renderType, doRender);
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
        graphics.pose().pushMatrix();
        graphics.pose().translation(
            Rect.xCoordinateFor(point.x, maxWidth, xAlignment) + maxWidth * 0.5f,
            Rect.yCoordinateFor(point.y, fontRenderer.lineHeight, yAlignment) + fontRenderer.lineHeight * 0.5f
        );
        if(scale < 1) graphics.pose().scale(scale, scale);
        graphics.drawString(
            fontRenderer,
            text,
            (maxWidth - Math.min(maxWidth, textWidth)) / 2,
            -fontRenderer.lineHeight / 2,
            color,
            dropShadow
        );
        graphics.pose().popMatrix();
    }

    public static void wrapInMatrixEntry(PoseStack matrixStack, Runnable thenDo) {
        matrixStack.pushPose();
        thenDo.run();
        matrixStack.popPose();
    }

}
