package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.minecraft.gui.utils.Rect;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;

import java.util.function.Consumer;

public class ItemButton extends Button {

    public static final int width = 20;
    public static final int height = 20;

    private static final int itemModelWidth = 16;
    private static final int itemModelHeight = 16;

    public ItemStack stack;
    private final ItemRenderer itemRenderer;
    private final FontRenderer font;

    public ItemButton(
        int x, int y, Rect.XAlignment xAlignment, Rect.YAlignment yAlignment, ItemStack stack,
        Consumer<ItemButton> pressedAction, ItemRenderer itemRenderer, FontRenderer font
    ) {
        super(
            Rect.xCoordinateFor(x, width, xAlignment),
            Rect.yCoordinateFor(y, height, yAlignment),
            width, height,
            new StringTextComponent(""),
            b -> pressedAction.accept((ItemButton)b)
        );
        this.stack = stack;
        this.itemRenderer = itemRenderer;
        this.font = font;
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.renderButton(matrixStack, mouseX, mouseY, partialTicks);

        int xTL = x + (width - itemModelWidth) / 2;
        int yTL = y + (height - itemModelHeight) / 2;
        this.itemRenderer.renderItemAndEffectIntoGUI(stack, xTL, yTL);
        this.itemRenderer.renderItemOverlayIntoGUI(font, stack, xTL, yTL, null);
    }
}
