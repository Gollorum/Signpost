package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.blockpartdata.types.BlockPartRenderer;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.math.Angle;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.Collection;

public class GuiBlockPartRenderer extends AbstractWidget {

    private final Collection<BlockPartInstance> partsToRender;
    private final Point center;
    private Angle yaw;
    private Angle pitch;
    private float scale;

    public GuiBlockPartRenderer(Collection<BlockPartInstance> partsToRender, Point center, Angle yaw, Angle pitch, float scale) {
        super(center.x - widthFor(scale) / 2, center.y - heightFor(scale) / 2, widthFor(scale), heightFor(scale), Component.literal(""));
        this.partsToRender = partsToRender;
        this.center = center;
        this.yaw = yaw;
        this.pitch = pitch;
        this.scale = scale;
    }

    private static int widthFor(float scale) { return (int)(scale * 1.5f); }
    private static int heightFor(float scale) { return (int)(scale * 1.5f); }

    @Override
    public void renderWidget(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if(isHovered)
            GuiComponent.fill(matrixStack, getX(), getY(), getX() + width, getY() + height, 0x20ffffff);

        PoseStack ms = new PoseStack();
        RenderingUtil.wrapInMatrixEntry(ms, () -> {
            ms.translate(0, 0, 100);
            for(BlockPartInstance bpi : partsToRender) {
                BlockPartRenderer.renderGuiDynamic(
                    bpi.blockPart,
                    ms,
                    center, yaw, pitch, false, scale, bpi.offset.withY(y -> y - 0.5f)
                );
            }
        });
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        yaw = yaw.add(Angle.fromDegrees((float) (dragX * 3)));
        pitch = pitch.add(Angle.fromDegrees((float) (dragY * 3)));
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput p_169152_) {

    }
}
