package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.minecraft.gui.utils.Rect;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public class ItemButton extends Button {

    public static final int width = 20;
    public static final int height = 20;

    private static final int itemModelWidth = 16;
    private static final int itemModelHeight = 16;

    public ItemStack stack;
    private final ItemRenderer itemRenderer;
    private final Font font;

    public ItemButton(
        int x, int y, Rect.XAlignment xAlignment, Rect.YAlignment yAlignment, ItemStack stack,
        Consumer<ItemButton> pressedAction, ItemRenderer itemRenderer, Font font
    ) {
        super(
            Rect.xCoordinateFor(x, width, xAlignment),
            Rect.yCoordinateFor(y, height, yAlignment),
            width, height,
            Component.literal(""),
            b -> pressedAction.accept((ItemButton)b)
        );
        this.stack = stack;
        this.itemRenderer = itemRenderer;
        this.font = font;
    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.renderButton(matrixStack, mouseX, mouseY, partialTicks);

        int xTL = x + (width - itemModelWidth) / 2;
        int yTL = y + (height - itemModelHeight) / 2;
        this.itemRenderer.renderAndDecorateItem(stack, xTL, yTL);
        this.itemRenderer.renderGuiItemDecorations(font, stack, xTL, yTL, null);
    }
}
