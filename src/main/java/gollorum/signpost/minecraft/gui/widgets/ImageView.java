package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.gui.utils.TextureResource;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;

public class ImageView implements Renderable {

	private final TextureResource texture;
	private final Rect rect;


	public ImageView(TextureResource texture, Rect rect) {
		this.texture = texture;
		this.rect = rect;
	}

	@Override
	public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		RenderSystem.setShaderTexture(0, texture.location);
		AbstractWidget.blit(matrixStack, rect.point.x, rect.point.y, 0, texture.offset.width, texture.offset.height, rect.width, rect.height, texture.fileSize.width, texture.fileSize.height);
	}


}
