package gollorum.signpost.minecraft.gui.widgets;

import gollorum.signpost.minecraft.gui.utils.IConfigurableFont;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.gui.utils.WithMutableX;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class InputBox extends EditBox implements WithMutableX {//, Ticking {

    private boolean shouldDropShadow;

    private final IConfigurableFont configFont;

    private final List<Function<Integer, Boolean>> keyCodeConsumers = new ArrayList<>();

    private final double zOffset;

    public InputBox(
        Font configFont,
        Rect inputFieldRect,
        boolean shouldDropShadow,
        double zOffset
    ) {
        this(new Font(((IConfigurableFont) configFont).getFonts(), ((IConfigurableFont) configFont).getFilterFishyGlyphs()), true, inputFieldRect, shouldDropShadow, zOffset);
    }

    private InputBox(
        Font copyFont,
        boolean iCopiedThatFontIPromise,
        Rect inputFieldRect,
        boolean shouldDropShadow,
        double zOffset
    ) {
        super(
            copyFont,
            inputFieldRect.point.x, inputFieldRect.point.y,
            inputFieldRect.width, inputFieldRect.height,
            Component.literal("")
        );
        this.configFont = (IConfigurableFont) copyFont;
        if(!shouldDropShadow) configFont.setShouldProhibitShadows(true);
        this.shouldDropShadow = shouldDropShadow;
        this.zOffset = zOffset;
    }

//    @Override
//    public void doTick() {
//        super.tick();
//    }

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
    public void renderWidget(GuiGraphics graphics, int p_94161_, int p_94162_, float p_94163_) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, zOffset);
        if(isHovered && !isBordered()) {
            int fromY = getY() + (((Font)configFont).lineHeight - height) / 2;
            graphics.fill(RenderType.guiOverlay(), getX(), fromY, getX() + width, fromY + height, 0x40ffffff);
        }
        super.renderWidget(graphics, p_94161_, p_94162_, p_94163_);
        graphics.pose().popPose();
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