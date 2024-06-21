package gollorum.signpost.minecraft.gui.widgets;

import gollorum.signpost.minecraft.gui.utils.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class GuiItemRenderer extends AbstractWidget {

	private ItemStack itemStack;

	private final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

	public GuiItemRenderer(Rect rect, ItemStack itemStack) {
		super(rect.point.x, rect.point.y, rect.width, rect.height, Component.literal("GuiItemRenderer"));
		this.itemStack = itemStack;
	}

	public void setItemStack(ItemStack itemStack) {
		this.itemStack = itemStack;
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		Font font = Minecraft.getInstance().font;
		graphics.renderItem(itemStack, getX(), getY());
		graphics.renderItemDecorations(font, itemStack, getX(), getY(), null);
	}

	@Override
	public void updateWidgetNarration(NarrationElementOutput p_169152_) {

	}
}
