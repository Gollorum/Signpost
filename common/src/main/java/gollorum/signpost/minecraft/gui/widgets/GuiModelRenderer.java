package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.minecraft.gui.utils.Flippable;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.rendering.FlippableModel;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.AxisAngle4d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class GuiModelRenderer implements Renderable, Flippable {

    private final FlippableModel model;
    private final float modelSpaceXOffset;
    private final float modelSpaceYOffset;
    private boolean isFlipped = false;

    private final Point center;
    private final int width;
    private final int height;

    public final Rect rect;

    public GuiModelRenderer(Rect rect, FlippableModel model, float modelSpaceXOffset, float modelSpaceYOffset) {
        this.rect = rect;
        center = rect.center();
        width = rect.width;
        height = rect.height;
        this.model = model;
        this.modelSpaceXOffset = modelSpaceXOffset;
        this.modelSpaceYOffset = modelSpaceYOffset;
    }


    public boolean isFlipped() {
        return isFlipped;
    }

    public void setFlipped(boolean isFlipped) {
        this.isFlipped = isFlipped;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        float scale = Math.min(width, height);
        PoseStack matrixStack = new PoseStack();
        matrixStack.translate(center.x, center.y, 0);
        matrixStack.scale(scale, -scale, scale);
        if (isFlipped) matrixStack.mulPose(new Quaternionf(new AxisAngle4d(Math.PI, new Vector3f(0, 1, 0))));
        matrixStack.translate(modelSpaceXOffset, modelSpaceYOffset, 0);
        // 1.21.1 draws straight into the gui's buffer source; flush so the quads land in this widget's
        // z order rather than at the end of the frame.
        for (var moo : model.get(isFlipped))
            RenderingUtil.render(
                matrixStack,
                moo,
                graphics.bufferSource(),
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                t -> RenderType.cutout()
            );
        graphics.flush();
    }
}
