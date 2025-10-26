package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.vertex.*;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.models.modelGeneration.QuadModel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.*;

import java.lang.Math;
import java.util.function.Function;

public class RenderingUtil {

    public static void render(
        PoseStack blockToView,
        TexturedModel model,
        MultiBufferSource buffer,
        int combinedLights,
        int combinedOverlay,
        Function<ResourceLocation, RenderType> renderTypeFactory
    ) {
        render(blockToView,
            model.model(),
            model.texture().buffer(buffer, renderTypeFactory),
            combinedLights,
            combinedOverlay,
            model.tint());
    }

    public static void render(
        PoseStack blockToView,
        ModelPart model,
        VertexConsumer buffer,
        int combinedLights,
        int combinedOverlay,
        int color
    ) {
        model.render(blockToView, buffer, combinedLights, combinedOverlay, color);
    }

    // copied from ModelPart.render, sort of
    public static void render(
        PoseStack poseStack,
        QuadModel model,
        VertexConsumer buffer,
        int combinedLights,
        int combinedOverlay,
        int color
    ) {
        var pose = poseStack.last();
        var matrix = pose.pose();
        var bufferVector = new Vector3f();

        for (var quad : model.quads()) {
            pose.transformNormal(quad.normal(), bufferVector);
            var normalX = bufferVector.x;
            var normalY = bufferVector.y;
            var normalZ = bufferVector.z;

            for (var vertex : quad.vertices()) {
                matrix.transformPosition(vertex.pos(), bufferVector);
                buffer.addVertex(
                    bufferVector.x(), bufferVector.y(), bufferVector.z(),
                    color,
                    vertex.u(), vertex.v(),
                    combinedOverlay, combinedLights,
                    normalX, normalY, normalZ);
            }
        }
    }

    public static void drawString(GuiGraphics graphics, Font fontRenderer, String text, Point point, Rect.XAlignment xAlignment, Rect.YAlignment yAlignment, int color, int maxWidth, boolean dropShadow){
        graphics.drawSpecial(buffer -> {
            int textWidth = fontRenderer.width(text);
            float scale = Math.min(1f, maxWidth / (float) textWidth);
            Matrix4f matrix = new Matrix4f().translation(
                Rect.xCoordinateFor(point.x, maxWidth, xAlignment) + maxWidth * 0.5f,
                Rect.yCoordinateFor(point.y, fontRenderer.lineHeight, yAlignment) + fontRenderer.lineHeight * 0.5f,
                100
            );
            if(scale < 1) matrix.scale(scale, scale, scale);
            fontRenderer.drawInBatch(
                text,
                (maxWidth - Math.min(maxWidth, textWidth)) * 0.5f,
                -fontRenderer.lineHeight * 0.5f,
                color,
                dropShadow,
                matrix,
                buffer,
                Font.DisplayMode.NORMAL,
                0,
                0xf000f0
            );
        });
    }

    public static void wrapInMatrixEntry(PoseStack matrixStack, Runnable thenDo) {
        matrixStack.pushPose();
        thenDo.run();
        matrixStack.popPose();
    }

}
