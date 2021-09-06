package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.minecraft.gui.utils.ConfigurableFont;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.gui.utils.Ticking;
import gollorum.signpost.minecraft.gui.utils.WithMutableX;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class InputBox extends TextFieldWidget implements WithMutableX, Ticking {

    private boolean shouldDropShadow;

    private final ConfigurableFont configFont;

    private final List<Function<Integer, Boolean>> keyCodeConsumers = new ArrayList<>();

    private final double zOffset;

    public InputBox(
        FontRenderer configFont,
        Rect inputFieldRect,
        boolean shouldDropShadow,
        boolean shouldRenderBackGround,
        double zOffset
    ) {
        this(new ConfigurableFont(configFont, !shouldDropShadow), inputFieldRect, shouldDropShadow, shouldRenderBackGround, zOffset);
    }

    private InputBox(
        ConfigurableFont configFont,
        Rect inputFieldRect,
        boolean shouldDropShadow,
        boolean shouldRenderBackGround,
        double zOffset
    ) {
        super(
            configFont,
            inputFieldRect.point.x, inputFieldRect.point.y,
            inputFieldRect.width, inputFieldRect.height,
            new StringTextComponent("")
        );
        this.configFont = configFont;
        this.shouldDropShadow = shouldDropShadow;
        this.zOffset = zOffset;
        setBordered(shouldRenderBackGround);
    }

    @Override
    public void doTick() {
        super.tick();
    }

    public boolean shouldDropShadow() {
        return shouldDropShadow;
    }

    public void setShouldDropShadow(boolean shouldDropShadow) {
        this.shouldDropShadow = shouldDropShadow;
        configFont.setShouldProhibitShadows(!shouldDropShadow);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for(Function<Integer, Boolean> consumer : keyCodeConsumers) {
            if(consumer.apply(keyCode)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
    public void renderButton(MatrixStack matrixStack, int p_94161_, int p_94162_, float p_94163_) {
        matrixStack.pushPose();
        matrixStack.translate(0, 0, zOffset);
        super.renderButton(matrixStack, p_94161_, p_94162_, p_94163_);
        matrixStack.popPose();
    }

    @Override
    public void setBordered(boolean shouldBeBordered) {
        super.setBordered(shouldBeBordered);
        if(!shouldBeBordered) {
            y += (this.height - 8) / 2;
        }
        else {
            y -= (this.height - 8) / 2;
        }
    }

    public String getText() { return getValue(); }
    public void setText(String txt) { setValue(txt); }

    @Override
    public int getXPos() {
        return x;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public void setXPos(int x) {
        this.x = x;
    }
}