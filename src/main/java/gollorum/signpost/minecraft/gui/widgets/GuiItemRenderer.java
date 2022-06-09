package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.minecraft.gui.utils.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.RenderProperties;

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
	public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		Font font = RenderProperties.get(itemStack).getFont(itemStack);
		if (font == null) font = Minecraft.getInstance().font;
		this.setBlitOffset(200);
		this.itemRenderer.blitOffset = 200.0F;
		itemRenderer.renderAndDecorateItem(itemStack, x, y);
		itemRenderer.renderGuiItemDecorations(font, itemStack, x, y, null);
		this.itemRenderer.blitOffset = 0.0F;
		this.setBlitOffset(0);
	}

	@Override
	public void updateNarration(NarrationElementOutput p_169152_) {

	}
}
