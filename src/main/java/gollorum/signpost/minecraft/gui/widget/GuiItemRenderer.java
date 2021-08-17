package gollorum.signpost.minecraft.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.minecraft.gui.utils.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;

public class GuiItemRenderer extends Widget {

	private ItemStack itemStack;

	private final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

	public GuiItemRenderer(Rect rect, ItemStack itemStack) {
		super(rect.point.x, rect.point.y, rect.width, rect.height, new StringTextComponent("GuiItemRenderer"));
		this.itemStack = itemStack;
	}

	public void setItemStack(ItemStack itemStack) {
		this.itemStack = itemStack;
	}

	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		FontRenderer font = itemStack.getItem().getFontRenderer(itemStack);
		if (font == null) font = Minecraft.getInstance().fontRenderer;
		this.setBlitOffset(200);
		this.itemRenderer.zLevel = 200.0F;
		itemRenderer.renderItemAndEffectIntoGUI(itemStack, x, y);
		itemRenderer.renderItemOverlayIntoGUI(font, itemStack, x, y, null);
		this.itemRenderer.zLevel = 0.0F;
		this.setBlitOffset(0);
	}
}
