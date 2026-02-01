package gollorum.signpost.minecraft.gui.widgets;

import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.gui.utils.TextureResource;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ColorInputBox extends InputBox {

    private int currentResult;

    public ColorInputBox(Font fontRenderer, Rect inputFieldRect, double zOffset) {
        super(fontRenderer,
            new Rect(
                new Point(inputFieldRect.point.x + inputFieldRect.height, inputFieldRect.point.y),
                inputFieldRect.width - inputFieldRect.height, inputFieldRect.height
            ),
            true
        );
        setFilter(null);
        setResponder(null);
        setValue("#000000");
    }

    private static boolean isValidColor(String text) {
        return text.startsWith("#") && text.length() <= 7 && canParse(text.substring(1));
    }

    @Override
    public void setFilter(Predicate<String> filter) {
        super.setFilter(text -> isValidColor(text) && (filter == null || filter.test(text)));
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
        if(getValue().equals("#")) return 0;
        return Integer.parseInt(getValue().substring(1), 16);
    }

    public int getCurrentColor() { return Colors.withAlpha(currentResult, 0xff); }

    @Override
    public void setResponder(Consumer<String> responder) {
        super.setResponder(text -> {
            currentResult = getResult();
            if(responder != null) {
                responder.accept(text);
            }
        });
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            TextureResource.paintBackground.location,
            getX() - height, getY(),
            0, 0,
            height, height,
            TextureResource.paintBackground.size.width, TextureResource.paintBackground.size.height,
            getCurrentColor()
        );
        super.renderWidget(graphics, mouseX, mouseY, partialTicks);
    }

    public void setColorResponder(Consumer<Integer> responder) {
        setResponder(text -> {
            if(responder != null) responder.accept(getCurrentColor());
        });
    }

    public void setSelectedColor(int color) {
        String text = Integer.toHexString(color);
        if(text.length() < 6) {
            text = String.join("", Collections.nCopies(6 - text.length(), "0")) + text;
        }
        setValue("#" + text);
    }
}
