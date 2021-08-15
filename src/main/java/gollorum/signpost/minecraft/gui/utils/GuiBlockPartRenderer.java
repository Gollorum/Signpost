package gollorum.signpost.minecraft.gui.utils;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.blockpartdata.types.renderers.BlockPartRenderer;
import gollorum.signpost.utils.BlockPartInstance;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.StringTextComponent;

import java.util.Collection;

public class GuiBlockPartRenderer extends Widget implements IRenderable {

    private final Collection<BlockPartInstance> partsToRender;
    private final Point center;
    private float yaw;
    private float pitch;
    private float scale;

    public GuiBlockPartRenderer(Collection<BlockPartInstance> partsToRender, Point center, float yaw, float pitch, float scale) {
        super(center.x - widthFor(scale) / 2, center.y - heightFor(scale) / 2, widthFor(scale), heightFor(scale), new StringTextComponent(""));
        this.partsToRender = partsToRender;
        this.center = center;
        this.yaw = yaw;
        this.pitch = pitch;
        this.scale = scale;
    }

    private static int widthFor(float scale) { return (int)(scale * 1.5f); }
    private static int heightFor(float scale) { return (int)(scale * 1.5f); }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if(isHovered())
            AbstractGui.fill(matrixStack, x, y, x + width, y + height, 0x20ffffff);
        for(BlockPartInstance bpi : partsToRender) {
            BlockPartRenderer.renderGuiDynamic(
                bpi.blockPart,
                center, yaw, pitch, scale, bpi.offset.add(0, -0.5f, 0.5f)
            );
        }
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        yaw += dragX * 3;
        pitch += dragY * 3;
    }
}
