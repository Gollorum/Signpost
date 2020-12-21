package gollorum.signpost.minecraft.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IRenderable;

public class TextDisplay implements IRenderable {

    private final String text;
    private final Point point;
    private final FontRenderer fontRenderer;

    public TextDisplay(String text, Point point, Rect.XAlignment xAlignment, Rect.YAlignment yAlignment, FontRenderer fontRenderer) {
        this.text = text;
        this.point = new Rect(point, fontRenderer.getStringWidth(text), fontRenderer.FONT_HEIGHT, xAlignment, yAlignment).point;
        this.fontRenderer = fontRenderer;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        fontRenderer.drawStringWithShadow(matrixStack, text, point.x, point.y, Colors.white);
    }
}
