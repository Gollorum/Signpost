package gollorum.signpost.minecraft.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.gui.utils.TextureResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.function.Consumer;

public class ColorInputBox extends InputBox {

    @Nullable
    private Consumer<Integer> responder;

    private int currentResult;

    public ColorInputBox(FontRenderer fontRenderer, Rect inputFieldRect, double zOffset) {
        super(fontRenderer, new Rect(
            new Point(inputFieldRect.point.x + inputFieldRect.height, inputFieldRect.point.y),
            inputFieldRect.width - inputFieldRect.height, inputFieldRect.height
        ), true, true, zOffset);
        setValidator(text -> text.startsWith("#") && text.length() <= 7 && canParse(text.substring(1)));
        setText("#000000");
    }

    private static boolean canParse(String text) {
        if(text.equals("")) return true;
        try {
            Integer.parseInt(text, 16);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    private int getResult() {
        if(getText().equals("#")) return 0;
        return Integer.parseInt(getText().substring(1), 16);
    }

    public int getCurrentColor() { return currentResult; }

    @Override
    protected void onTextChanged() {
        currentResult = getResult();
        if(responder != null) {
            responder.accept(currentResult);
        }
        super.onTextChanged();
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        Minecraft.getInstance().getTextureManager().bindTexture(TextureResource.background.location);
        int red = Colors.getRed(currentResult);
        int green = Colors.getGreen(currentResult);
        int blue = Colors.getBlue(currentResult);
        RenderSystem.color4f(1, 1, 1, 1);
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        bufferbuilder.pos(x - height, y + height, 0.0D).tex(0, 1).color(red, green, blue, 255).endVertex();
        bufferbuilder.pos(x, y + height, 0.0D).tex(1, 1).color(red, green, blue, 255).endVertex();
        bufferbuilder.pos(x, y, 0.0D).tex(1, 0).color(red, green, blue, 255).endVertex();
        bufferbuilder.pos(x - height, y, 0.0D).tex(0, 0).color(red, green, blue, 255).endVertex();
        tessellator.draw();
        super.renderButton(matrixStack, mouseX, mouseY, partialTicks);
    }

    public void setColorResponder(@Nullable Consumer<Integer> responder) {
        this.responder = responder;
    }

    public void setSelectedColor(int color) {
        String text = Integer.toHexString(color);
        if(text.length() < 6) {
            text = String.join("", Collections.nCopies(6 - text.length(), "0")) + text;
        }
        setText("#" + text);
    }
}
