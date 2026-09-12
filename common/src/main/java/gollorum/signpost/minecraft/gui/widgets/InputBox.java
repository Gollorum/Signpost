package gollorum.signpost.minecraft.gui.widgets;

import gollorum.signpost.minecraft.gui.utils.IConfigurableFont;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.gui.utils.WithMutableX;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

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
            copyOf(configFont),
            inputFieldRect,
            shouldDropShadow,
            zOffset,
            500
        );
    }

    /**
     * 1.21.1's {@link EditBox} always draws through the shadowed {@code drawString} overload and has no
     * {@code setTextShadow}, so the shadow is suppressed on the font instead - see
     * {@link IConfigurableFont}. The box gets its own copy of the font because that flag is per-Font:
     * setting it on Minecraft's shared instance would strip shadows from every piece of text in the game.
     */
    private static Font copyOf(Font font) {
        IConfigurableFont configurable = (IConfigurableFont) font;
        return new Font(configurable.getFonts(), configurable.getFilterFishyGlyphs());
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
        this.zOffset = zOffset;
        ((IConfigurableFont) copyFont).setShouldProhibitShadows(!shouldDropShadow);
        this.setMaxLength(maxStringLength);
    }

//    @Override
//    public void doTick() {
//        super.tick();
//    }

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

    /**
     * Everything this widget draws is batched into the screen's buffer source and only flushed at the
     * end of the frame, so it competes on <em>depth</em> rather than on draw order with anything that
     * flushes earlier - notably {@link GuiModelRenderer}, which draws the 3D sign preview the sign text
     * boxes sit on top of. At equal depth the model wins and the text and hover highlight vanish, while
     * the caret survives because {@code EditBox} draws it through {@code RenderType.guiOverlay()},
     * which ignores depth. Hence the explicit z offset: callers that overlap a model pass one large
     * enough to clear it.
     */
    @Override
    public void renderWidget(GuiGraphics graphics, int p_94161_, int p_94162_, float p_94163_) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, zOffset);
        if(isHovered && !isBordered()) {
            int fromY = getY() + (configFont.lineHeight - height) / 2;
            graphics.fill(getX(), fromY, getX() + width, fromY + height, 0x40ffffff);
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
