package gollorum.signpost.minecraft.gui.widgets;

import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.gui.utils.WithMutableX;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

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

    /**
     * Rejects any edit whose resulting text fails this predicate.
     *
     * <p>26.1 removed {@code EditBox#setFilter}, and the {@code TextFormatter} that replaced it
     * only styles the displayed text - it cannot refuse input. So the old behaviour is
     * reimplemented here around the same three methods vanilla used to gate: the candidate value
     * is built and tested before the edit is applied, so a rejected keystroke changes nothing at
     * all (no value change, no cursor movement and no responder call).
     */
    public void setFilter(Predicate<String> filter) {
        this.filter = filter == null ? s -> true : filter;
    }

    private Predicate<String> filter = s -> true;

    /** Start index of the current selection, derived from the cursor and the highlighted text. */
    private int selectionStart() {
        int cursor = getCursorPosition();
        int length = getHighlighted().length();
        if(length == 0) return cursor;
        return getValue().regionMatches(cursor, getHighlighted(), 0, length) ? cursor : cursor - length;
    }

    @Override
    public void setValue(String value) {
        if(filter.test(value)) super.setValue(value);
    }

    @Override
    public void insertText(String input) {
        int start = selectionStart();
        int end = start + getHighlighted().length();
        String candidate = new StringBuilder(getValue())
            .replace(start, end, StringUtil.filterText(input))
            .toString();
        if(filter.test(candidate)) super.insertText(input);
    }

    @Override
    public void deleteCharsToPos(int pos) {
        if(getValue().isEmpty()) return;
        String candidate;
        if(!getHighlighted().isEmpty()) {
            int start = selectionStart();
            candidate = new StringBuilder(getValue()).delete(start, start + getHighlighted().length()).toString();
        } else {
            int cursor = getCursorPosition();
            int start = Math.min(pos, cursor);
            int end = Math.max(pos, cursor);
            if(start == end) return;
            candidate = new StringBuilder(getValue()).delete(start, end).toString();
        }
        if(filter.test(candidate)) super.deleteCharsToPos(pos);
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
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int p_94161_, int p_94162_, float p_94163_) {
        if(isHovered && !isBordered()) {
            int fromY = getY() + (configFont.lineHeight - height) / 2;
            graphics.fill(RenderPipelines.GUI, getX(), fromY, getX() + width, fromY + height, 0x40ffffff);
        }
        super.extractWidgetRenderState(graphics, p_94161_, p_94162_, p_94163_);
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