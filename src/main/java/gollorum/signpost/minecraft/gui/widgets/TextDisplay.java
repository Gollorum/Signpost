package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;

public class TextDisplay implements Renderable {

    private final Component text;
    public final Rect rect;
    private final Font fontRenderer;

    public TextDisplay(Component text, Point point, Rect.XAlignment xAlignment, Rect.YAlignment yAlignment, Font fontRenderer) {
        this.text = text;
        this.rect = new Rect(
            point,
            fontRenderer.width(text), fontRenderer.lineHeight,
            xAlignment, yAlignment
        );
        this.fontRenderer = fontRenderer;
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        int x = rect.point.x;
        fontRenderer.drawShadow(matrixStack, text, x, rect.point.y, Colors.white);
    }

}
