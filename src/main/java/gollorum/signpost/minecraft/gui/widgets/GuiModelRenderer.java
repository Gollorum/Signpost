package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.minecraft.gui.utils.Flippable;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.rendering.FlippableModel;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.RenderType;
import org.joml.AxisAngle4d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nonnull;

public class GuiModelRenderer implements Renderable, Flippable {

    private final FlippableModel model;
    private final float modelSpaceXOffset;
    private final float modelSpaceYOffset;
    private boolean isFlipped = false;

    private final Point center;
    private final int width;
    private final int height;

    public final Rect rect;
    private final RenderType renderType;

    private final int color;

    public GuiModelRenderer(Rect rect, FlippableModel model, float modelSpaceXOffset, float modelSpaceYOffset, RenderType renderType) {
        this(rect, model, modelSpaceXOffset, modelSpaceYOffset, renderType, 0xffffff);
    }

    public GuiModelRenderer(Rect rect, FlippableModel model, float modelSpaceXOffset, float modelSpaceYOffset, RenderType renderType, int color) {
        this.rect = rect;
        center = rect.center();
        width = rect.width;
        height = rect.height;
        this.model = model;
        this.modelSpaceXOffset = modelSpaceXOffset;
        this.modelSpaceYOffset = modelSpaceYOffset;
        this.renderType = renderType;
        this.color = color;
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
        RenderSystem.enableBlend();
        RenderingUtil.wrapInMatrixEntry(matrixStack, () -> {
            matrixStack.translate(0, 0, -10);
            if(isFlipped) matrixStack.mulPose(new Quaternionf(new AxisAngle4d(Math.PI, new Vector3f(0, 1, 0))));
            RenderingUtil.renderGui(
                model.get(isFlipped),
                new PoseStack(),
                color,
                center,
                Angle.ZERO,
                Angle.ZERO,
                isFlipped,
                scale,
                new Vector3(modelSpaceXOffset, modelSpaceYOffset, 0),
                renderType,
                m -> {}
            );
        });
    }

}
