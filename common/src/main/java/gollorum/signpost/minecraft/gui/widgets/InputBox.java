package gollorum.signpost.minecraft.gui.widgets;

import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.gui.utils.WithMutableX;
import gollorum.signpost.mixin.GuiGraphicsMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class InputBox extends EditBox implements WithMutableX {//, Ticking {

    private boolean shouldDropShadow;

    private final Font configFont;

    private final List<Function<Integer, Boolean>> keyCodeConsumers = new ArrayList<>();

    private final double zOffset;

    public InputBox(
        Font configFont,
        Rect inputFieldRect,
        boolean shouldDropShadow,
        double zOffset
    ) {
        this(
            configFont,
            inputFieldRect,
            shouldDropShadow,
            zOffset,
            500
        );
    }

    private InputBox(
        Font copyFont,
        Rect inputFieldRect,
        boolean shouldDropShadow,
        double zOffset,
        int maxStringLength
    ) {
        super(
            copyFont,
            inputFieldRect.point.x, inputFieldRect.point.y,
            inputFieldRect.width, inputFieldRect.height,
            Component.literal("")
        );
        this.configFont = copyFont;
        this.shouldDropShadow = shouldDropShadow;
        setTextShadow(shouldDropShadow);
        this.zOffset = zOffset;
        this.setMaxLength(maxStringLength);
    }

//    @Override
//    public void doTick() {
//        super.tick();
//    }

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
        if (!shouldDropShadow)
            graphics = new NoShadowGuiGraphics(graphics);
//        graphics.pose().pushMatrix();
//        graphics.pose().translate(0, 0, zOffset);
        if(isHovered && !isBordered()) {
            int fromY = getY() + (configFont.lineHeight - height) / 2;
            graphics.fill(RenderPipelines.GUI, getX(), fromY, getX() + width, fromY + height, 0x40ffffff);
        }
        super.renderWidget(graphics, p_94161_, p_94162_, p_94163_);
//        graphics.pose().popMatrix();
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

    private class NoShadowGuiGraphics extends GuiGraphics {

        public NoShadowGuiGraphics(GuiGraphics original) {
            super(Minecraft.getInstance(), ((GuiGraphicsMixin)original).getGuiRenderState());
        }

        @Override
        public void drawString(Font font, @Nullable String text, int x, int y, int color) {
            super.drawString(font, text, x, y, color, false);
        }

        @Override
        public void drawString(Font font, FormattedCharSequence text, int x, int y, int color) {
            super.drawString(font, text, x, y, color, false);
        }

        @Override
        public void drawString(Font font, Component text, int x, int y, int color) {
            super.drawString(font, text, x, y, color, false);
        }

        @Override
        public void drawWordWrap(Font font, FormattedText text, int x, int y, int lineWidth, int color) {
            drawWordWrap(font, text, x, y, lineWidth, color, false);
        }
    }
}