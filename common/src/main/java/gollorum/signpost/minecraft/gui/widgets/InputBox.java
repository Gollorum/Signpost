package gollorum.signpost.minecraft.gui.widgets;

import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.gui.utils.WithMutableX;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class InputBox extends EditBox implements WithMutableX {

    private final Font configFont;

    private final List<Function<Integer, Boolean>> keyCodeConsumers = new ArrayList<>();

    public InputBox(
        Font configFont,
        Rect inputFieldRect,
        boolean shouldDropShadow
    ) {
        this(
            configFont,
            inputFieldRect,
            shouldDropShadow,
            500
        );
    }

    private InputBox(
        Font copyFont,
        Rect inputFieldRect,
        boolean shouldDropShadow,
        int maxStringLength
    ) {
        super(
            copyFont,
            inputFieldRect.point.x, inputFieldRect.point.y,
            inputFieldRect.width, inputFieldRect.height,
            Component.literal("")
        );
        this.configFont = copyFont;
        setTextShadow(shouldDropShadow);
        this.setMaxLength(maxStringLength);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        for(Function<Integer, Boolean> consumer : keyCodeConsumers) {
            if(consumer.apply(event.key())) return true;
        }
        return super.keyPressed(event);
    }

    public void addKeyCodeListener(int keyCode, Runnable action) {
        keyCodeConsumers.add(i -> {
            if(i == keyCode) {
                action.run();
                return true;
            } else return false;
        });
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int p_94161_, int p_94162_, float p_94163_) {
        if(isHovered && !isBordered()) {
            int fromY = getY() + (configFont.lineHeight - height) / 2;
            graphics.fill(RenderPipelines.GUI, getX(), fromY, getX() + width, fromY + height, 0x40ffffff);
        }
        super.renderWidget(graphics, p_94161_, p_94162_, p_94163_);
    }

    @Override
    public void setBordered(boolean shouldBeBordered) {
        super.setBordered(shouldBeBordered);
        setY(getY() + (shouldBeBordered ? -(this.height - 8) / 2 : (this.height - 8) / 2));
    }

    @Override
    public int getXPos() {
        return getX();
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public void setXPos(int x) {
        setX(x);
    }

}