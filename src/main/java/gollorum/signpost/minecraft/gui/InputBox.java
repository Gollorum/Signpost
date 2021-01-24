package gollorum.signpost.minecraft.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import gollorum.signpost.Signpost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class InputBox extends Widget implements IRenderable, IGuiEventListener, WithMutableX, Ticking {
    private final FontRenderer fontRenderer;
    /** Has the current text being edited on the textbox. */
    private String text = "";
    private int maxStringLength = 200;
    private int cursorCounter;
    private boolean enableBackgroundDrawing = true;
    /** If this value is true along with isFocused, keyTyped will process the keys. */
    private boolean isEnabled = true;
    private boolean isShiftDown;
    /** The current character index that should be used as start of the rendered text. */
    private int lineScrollOffset;
    private int cursorPosition;
    /** other selection position, maybe the same as the cursor */
    private int selectionEnd;
    private int enabledColor = Colors.lightGrey;
    private int disabledColor = Colors.darkGrey;

    public int backgroundHoverColor = 0x60ffffff;
    public boolean hintBackgroundOnHover = true;

    private boolean shouldDropShadow;

    private String suggestion;
    private Consumer<String> onTextChanged;
    /** Called to check if the text is valid */
    private Predicate<String> validator = Objects::nonNull;
    private BiFunction<String, Integer, IReorderingProcessor> textFormatter = (text, offset) ->
        IReorderingProcessor.fromString(text, Style.EMPTY);
    private final double zOffset;

    private final List<Function<Integer, Boolean>> keyCodeConsumers = new ArrayList<>();

    public InputBox(
        FontRenderer fontRenderer,
        Rect inputFieldRect,
        boolean shouldDropShadow,
        boolean shouldRenderBackGround,
        double zOffset) {
        this(
            fontRenderer,
            inputFieldRect.point.x, inputFieldRect.point.y,
            inputFieldRect.width, inputFieldRect.height,
            shouldDropShadow,
            new StringTextComponent(""),
            zOffset
        );
        this.setEnableBackgroundDrawing(shouldRenderBackGround);
    }
    public InputBox(FontRenderer fontIn, int xIn, int yIn, int widthIn, int heightIn, boolean shouldDropShadow, ITextComponent msg, double zOffset) {
        super(xIn, yIn, widthIn, heightIn, msg);
        this.fontRenderer = fontIn;
        this.zOffset = zOffset;
        this.shouldDropShadow = shouldDropShadow;
    }

    public int getHeight() { return height; }

    public void setTextChangedCallback(Consumer<String> onTextChanged) {
        this.onTextChanged = onTextChanged;
    }

    @Override
    public void tick() {
        ++this.cursorCounter;
    }

    /**
     * Sets the text of the textbox, and moves the cursor to the end.
     */
    public void setText(String textIn) {
        if (this.validator.test(textIn)) {
            if (textIn.length() > this.maxStringLength) {
                this.text = textIn.substring(0, this.maxStringLength);
            } else {
                this.text = textIn;
            }

            this.setCursorPositionEnd();
            this.setSelectionPos(this.cursorPosition);
            this.onTextChanged();
        }
    }

    /**
     * Returns the contents of the textbox
     */
    public String getText() {
        return this.text;
    }

    /**
     * returns the text between the cursor and selectionEnd
     */
    public String getSelectedText() {
        return this.text.substring(
            Math.min(this.cursorPosition, this.selectionEnd),
            Math.max(this.cursorPosition, this.selectionEnd)
        );
    }

    public void setValidator(Predicate<String> validatorIn) {
        this.validator = validatorIn;
    }

    /**
     * Adds the given text after the cursor, or replaces the currently selected text if there is a selection.
     */
    public void writeText(String textToWrite) {
        int selectionFrom = Math.min(this.cursorPosition, this.selectionEnd);
        int selectionTo = Math.max(this.cursorPosition, this.selectionEnd);
        int availableSpace = this.maxStringLength - this.text.length() - (selectionFrom - selectionTo);
        String text = SharedConstants.filterAllowedCharacters(textToWrite);
        int usedUpSpace = text.length();
        if (availableSpace < usedUpSpace) {
            text = text.substring(0, availableSpace);
            usedUpSpace = availableSpace;
        }

        String newText = (new StringBuilder(this.text)).replace(selectionFrom, selectionTo, text).toString();
        if (this.validator.test(newText)) {
            this.text = newText;
            this.clampCursorPosition(selectionFrom + usedUpSpace);
            this.setSelectionPos(this.cursorPosition);
            this.onTextChanged();
        }
    }

    protected void onTextChanged() {
        if (this.onTextChanged != null) this.onTextChanged.accept(this.text);
    }

    private void delete(int chars) {
        if (Screen.hasControlDown()) {
            this.deleteWords(chars);
        } else {
            this.deleteFromCursor(chars);
        }

    }

    /**
     * Deletes the given number of words from the current cursor's position, unless there is currently a selection, in
     * which case the selection is deleted instead.
     */
    public void deleteWords(int num) {
        if (!this.text.isEmpty()) {
            if (this.selectionEnd != this.cursorPosition) {
                this.writeText("");
            } else {
                this.deleteFromCursor(this.getNthWordFromCursor(num) - this.cursorPosition);
            }
        }
    }

    /**
     * Deletes the given number of characters from the current cursor's position, unless there is currently a selection,
     * in which case the selection is deleted instead.
     */
    public void deleteFromCursor(int count) {
        if (!this.text.isEmpty()) {
            if (this.selectionEnd != this.cursorPosition) {
                this.writeText("");
            } else {
                int i = this.nThCharFromCursor(count);
                int min = Math.min(i, this.cursorPosition);
                int max = Math.max(i, this.cursorPosition);
                if (min != max) {
                    String s = (new StringBuilder(this.text)).delete(min, max).toString();
                    if (this.validator.test(s)) {
                        this.text = s;
                        this.setCursorPosition(min);
                    }
                }
            }
        }
    }

    /**
     * Gets the starting index of the word at the specified number of words away from the cursor position.
     */
    public int getNthWordFromCursor(int numWords) {
        return this.getNthWordFromPos(numWords, this.getCursorPosition());
    }

    /**
     * Gets the starting index of the word at a distance of the specified number of words away from the given position.
     */
    private int getNthWordFromPos(int n, int pos) {
        return this.getNthWordFromPosWS(n, pos, true);
    }

    /**
     * Like getNthWordFromPos (which wraps this), but adds option for skipping consecutive spaces
     */
    private int getNthWordFromPosWS(int n, int pos, boolean skipWs) {
        int i = pos;
        boolean flag = n < 0;
        int j = Math.abs(n);

        for(int k = 0; k < j; ++k) {
            if (!flag) {
                int l = this.text.length();
                i = this.text.indexOf(32, i);
                if (i == -1) {
                    i = l;
                } else {
                    while(skipWs && i < l && this.text.charAt(i) == ' ') {
                        ++i;
                    }
                }
            } else {
                while(skipWs && i > 0 && this.text.charAt(i - 1) == ' ') {
                    --i;
                }

                while(i > 0 && this.text.charAt(i - 1) != ' ') {
                    --i;
                }
            }
        }

        return i;
    }

    /**
     * Moves the text cursor by a specified number of characters and clears the selection
     */
    public void moveCursorBy(int chars) {
        this.setCursorPosition(this.nThCharFromCursor(chars));
    }

    private int nThCharFromCursor(int n) {
        return Util.func_240980_a_(this.text, this.cursorPosition, n);
    }

    /**
     * Sets the current position of the cursor.
     */
    public void setCursorPosition(int pos) {
        this.clampCursorPosition(pos);
        if (!this.isShiftDown) {
            this.setSelectionPos(this.cursorPosition);
        }
        cursorCounter = 0;

        this.onTextChanged();
    }

    public void clampCursorPosition(int pos) {
        this.cursorPosition = MathHelper.clamp(pos, 0, this.text.length());
    }

    /**
     * Moves the cursor to the very start of this text box.
     */
    public void setCursorPositionZero() {
        this.setCursorPosition(0);
    }

    /**
     * Moves the cursor to the very end of this text box.
     */
    public void setCursorPositionEnd() {
        this.setCursorPosition(this.text.length());
    }

    public void addKeyCodeListener(int keyCode, Runnable action) {
        keyCodeConsumers.add(i -> {
            if(i == keyCode) {
                action.run();
                return true;
            } else return false;
        });
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for(Function<Integer, Boolean> consumer : keyCodeConsumers) {
            if(consumer.apply(keyCode)) return true;
        }
        if (!this.canWrite()) {
            return false;
        } else {
            this.isShiftDown = Screen.hasShiftDown();
            if (Screen.isSelectAll(keyCode)) {
                this.setCursorPositionEnd();
                this.setSelectionPos(0);
                return true;
            } else if (Screen.isCopy(keyCode)) {
                Minecraft.getInstance().keyboardListener.setClipboardString(this.getSelectedText());
                return true;
            } else if (Screen.isPaste(keyCode)) {
                if (this.isEnabled) {
                    this.writeText(Minecraft.getInstance().keyboardListener.getClipboardString());
                }

                return true;
            } else if (Screen.isCut(keyCode)) {
                Minecraft.getInstance().keyboardListener.setClipboardString(this.getSelectedText());
                if (this.isEnabled) {
                    this.writeText("");
                }

                return true;
            } else {
                switch(keyCode) {
                    case KeyCodes.Del:
                        if (this.isEnabled) {
                            this.isShiftDown = false;
                            this.delete(-1);
                            this.isShiftDown = Screen.hasShiftDown();
                        }

                        return true;
                    case 260:
                    case 264:
                    case 265:
                    case 266:
                    case 267:
                    default:
                        return false;
                    case KeyCodes.Return:
                        if (this.isEnabled) {
                            this.isShiftDown = false;
                            this.delete(1);
                            this.isShiftDown = Screen.hasShiftDown();
                        }

                        return true;
                    case KeyCodes.Right:
                        if (Screen.hasControlDown()) {
                            this.setCursorPosition(this.getNthWordFromCursor(1));
                        } else {
                            this.moveCursorBy(1);
                        }

                        return true;
                    case KeyCodes.Left:
                        if (Screen.hasControlDown()) {
                            this.setCursorPosition(this.getNthWordFromCursor(-1));
                        } else {
                            this.moveCursorBy(-1);
                        }

                        return true;
                    case 268:
                        this.setCursorPositionZero();
                        return true;
                    case 269:
                        this.setCursorPositionEnd();
                        return true;
                }
            }
        }
    }

    public boolean canWrite() {
        return this.getVisible() && this.isFocused() && this.isEnabled();
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (!this.canWrite()) {
            return false;
        } else if (SharedConstants.isAllowedCharacter(codePoint)) {
            if (this.isEnabled) {
                this.writeText(Character.toString(codePoint));
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.getVisible()) {
            return false;
        } else {
            boolean wasClickedInsideBounds = mouseX >= (double)this.x && mouseX < (double)(this.x + this.width) && mouseY >= (double)this.y && mouseY < (double)(this.y + this.height);
            this.setFocused(wasClickedInsideBounds);

            if (this.isFocused() && wasClickedInsideBounds && button == 0) {
                int i = MathHelper.floor(mouseX) - this.x;
                if (this.enableBackgroundDrawing) {
                    i -= 4;
                }

                String s = this.fontRenderer.func_238412_a_(this.text.substring(this.lineScrollOffset), this.getAdjustedWidth());
                this.setCursorPosition(this.fontRenderer.func_238412_a_(s, i).length() + this.lineScrollOffset);
                return true;
            } else {
                return false;
            }
        }
    }

    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        matrixStack.push();
        matrixStack.translate(0, 0, zOffset);

        if (this.getVisible()) {
            if (this.getEnableBackgroundDrawing()) {
                fill(matrixStack, this.x - 1, this.y - 1, this.x + this.width + 1, this.y + this.height + 1, this.isFocused() ? 0xffffffff : 0xffa0a0a0);
                fill(matrixStack, this.x, this.y, this.x + this.width, this.y + this.height, 0xff000000);
            }
            if (hintBackgroundOnHover && isHovered) {
                fill(matrixStack, this.x, this.y, this.x + this.width, this.y + this.height, backgroundHoverColor);
            }

            int color = this.isEnabled ? this.enabledColor : this.disabledColor;
            int charsBeforeCursor = this.cursorPosition - this.lineScrollOffset;
            int charsBeforeSelectionEnd = this.selectionEnd - this.lineScrollOffset;
            String trimmedText = this.fontRenderer.func_238412_a_(this.text.substring(this.lineScrollOffset), this.getAdjustedWidth());
            boolean isCursorInsideText = charsBeforeCursor >= 0 && charsBeforeCursor <= trimmedText.length();
            boolean shouldRenderCursorBar = this.isFocused() && this.cursorCounter / 6 % 2 == 0 && isCursorInsideText;
            int textStartX = this.enableBackgroundDrawing ? this.x + 4 : this.x;
            int textY = y + (height - fontRenderer.FONT_HEIGHT) / 2;
            int currentXOffset = textStartX;
            if (charsBeforeSelectionEnd > trimmedText.length()) {
                charsBeforeSelectionEnd = trimmedText.length();
            }

            if (!trimmedText.isEmpty()) {
                String textBeforeCursor = isCursorInsideText ? trimmedText.substring(0, charsBeforeCursor) : trimmedText;
                currentXOffset = drawString(matrixStack, this.textFormatter.apply(textBeforeCursor, this.lineScrollOffset), textStartX, textY, color);
            }

            boolean isCursorBeforeEndOrTextTooLong = this.cursorPosition < this.text.length() || this.text.length() >= this.getMaxStringLength();
            int k1 = currentXOffset;
            if (!isCursorInsideText) {
                k1 = charsBeforeCursor > 0 ? textStartX + this.width : textStartX;
            } else if (isCursorBeforeEndOrTextTooLong) {
                k1 = --currentXOffset;
            }

            if (!trimmedText.isEmpty() && isCursorInsideText && charsBeforeCursor < trimmedText.length()) {
                drawString(matrixStack, this.textFormatter.apply(trimmedText.substring(charsBeforeCursor), this.cursorPosition), currentXOffset, textY, color);
            }

            if (!isCursorBeforeEndOrTextTooLong && this.suggestion != null) {
                drawString(matrixStack, this.suggestion, k1 - 1, textY, 0xff808080);
            }

            if (shouldRenderCursorBar) {
                if (isCursorBeforeEndOrTextTooLong) {
                    AbstractGui.fill(matrixStack, k1, textY - 1, k1 + 1, textY + 1 + 9, 0xffd0d0d0);
                } else {
                    drawString(matrixStack, "_", k1, textY, color);
                }
            }

            if (charsBeforeSelectionEnd != charsBeforeCursor) {
                int l1 = textStartX + this.fontRenderer.getStringWidth(trimmedText.substring(0, charsBeforeSelectionEnd));
                this.drawSelectionBox(k1, textY - 1, l1 - 1, textY + 1 + 9);
            }

        }

        matrixStack.pop();
    }

    private int drawString(MatrixStack matrixStack, String text, int x, int y, int color){
        return shouldDropShadow
            ? this.fontRenderer.drawStringWithShadow(matrixStack, text, x, y, color)
            : this.fontRenderer.drawString(matrixStack, text, x, y, color);
    }

    private int drawString(MatrixStack matrixStack, IReorderingProcessor processor, int x, int y, int color){
        return shouldDropShadow
            ? this.fontRenderer.func_238407_a_(matrixStack, processor, x, y, color)
            : this.fontRenderer.func_238422_b_(matrixStack, processor, x, y, color) + 1;
    }

    /**
     * Draws the blue selection box.
     */
    private void drawSelectionBox(int startX, int startY, int endX, int endY) {
        if (startX < endX) {
            int i = startX;
            startX = endX;
            endX = i;
        }

        if (startY < endY) {
            int j = startY;
            startY = endY;
            endY = j;
        }

        if (endX > this.x + this.width) {
            endX = this.x + this.width;
        }

        if (startX > this.x + this.width) {
            startX = this.x + this.width;
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        RenderSystem.color4f(0.0F, 0.0F, 255.0F, 255.0F);
        RenderSystem.disableTexture();
        RenderSystem.enableColorLogicOp();
        RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
        bufferbuilder.pos(startX, endY, 0.0D).endVertex();
        bufferbuilder.pos(endX, endY, 0.0D).endVertex();
        bufferbuilder.pos(endX, startY, 0.0D).endVertex();
        bufferbuilder.pos(startX, startY, 0.0D).endVertex();
        tessellator.draw();
        RenderSystem.disableColorLogicOp();
        RenderSystem.enableTexture();
        RenderSystem.color4f(255, 255, 255, 255);
    }

    /**
     * Sets the maximum length for the text in this text box. If the current text is longer than this length, the current
     * text will be trimmed.
     */
    public void setMaxStringLength(int length) {
        this.maxStringLength = length;
        if (this.text.length() > length) {
            this.text = this.text.substring(0, length);
            this.onTextChanged();
        }

    }

    /**
     * returns the maximum number of character that can be contained in this textbox
     */
    private int getMaxStringLength() {
        return this.maxStringLength;
    }

    /**
     * returns the current position of the cursor
     */
    public int getCursorPosition() {
        return this.cursorPosition;
    }

    /**
     * Gets whether the background and outline of this text box should be drawn (true if so).
     */
    private boolean getEnableBackgroundDrawing() {
        return this.enableBackgroundDrawing;
    }

    /**
     * Sets whether or not the background and outline of this text box should be drawn.
     */
    public void setEnableBackgroundDrawing(boolean enableBackgroundDrawingIn) {
        this.enableBackgroundDrawing = enableBackgroundDrawingIn;
    }

    /**
     * Sets the color to use when drawing this text box's text. A different color is used if this text box is disabled.
     */
    public void setTextColor(int color) {
        this.enabledColor = color;
    }

    /**
     * Sets the color to use for text in this text box when this text box is disabled.
     */
    public void setDisabledTextColour(int color) {
        this.disabledColor = color;
    }

    public boolean changeFocus(boolean focus) {
        return this.isEnabled & super.changeFocus(focus);
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible && mouseX >= (double)this.x && mouseX < (double)(this.x + this.width) && mouseY >= (double)this.y && mouseY < (double)(this.y + this.height);
    }

    protected void onFocusedChanged(boolean focused) {
        if (focused) {
            this.cursorCounter = 0;
        }

    }

    private boolean isEnabled() {
        return this.isEnabled;
    }

    /**
     * Sets whether this text box is enabled. Disabled text boxes cannot be typed in.
     */
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    /**
     * returns the width of the textbox depending on if background drawing is enabled
     */
    public int getAdjustedWidth() {
        return this.getEnableBackgroundDrawing() ? this.width - 8 : this.width;
    }

    /**
     * Sets the position of the selection anchor (the selection anchor and the cursor position mark the edges of the
     * selection). If the anchor is set beyond the bounds of the current text, it will be put back inside.
     */
    public void setSelectionPos(int position) {
        int i = this.text.length();
        this.selectionEnd = MathHelper.clamp(position, 0, i);
        if (this.fontRenderer != null) {
            if (this.lineScrollOffset > i) {
                this.lineScrollOffset = i;
            }

            int j = this.getAdjustedWidth();
            String s = this.fontRenderer.func_238412_a_(this.text.substring(this.lineScrollOffset), j);
            int k = s.length() + this.lineScrollOffset;
            if (this.selectionEnd == this.lineScrollOffset) {
                this.lineScrollOffset -= this.fontRenderer.func_238413_a_(this.text, j, true).length();
            }

            if (this.selectionEnd > k) {
                this.lineScrollOffset += this.selectionEnd - k;
            } else if (this.selectionEnd <= this.lineScrollOffset) {
                this.lineScrollOffset -= this.lineScrollOffset - this.selectionEnd;
            }

            this.lineScrollOffset = MathHelper.clamp(this.lineScrollOffset, 0, i);
        }

    }

    /**
     * returns true if this textbox is visible
     */
    public boolean getVisible() {
        return this.visible;
    }

    /**
     * Sets whether or not this textbox is visible
     */
    public void setVisible(boolean isVisible) {
        this.visible = isVisible;
    }

    public void setSuggestion(@Nullable String suggestion) {
        this.suggestion = suggestion;
    }

    public boolean shouldDropShadow() {
        return shouldDropShadow;
    }

    public void setShouldDropShadow(boolean shouldDropShadow) {
        this.shouldDropShadow = shouldDropShadow;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }
}