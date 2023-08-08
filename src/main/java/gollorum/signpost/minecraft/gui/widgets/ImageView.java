package gollorum.signpost.minecraft.gui.widgets;

import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.gui.utils.TextureResource;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;

public class ImageView implements Renderable {

	private final TextureResource texture;
	private final Rect rect;


	public ImageView(TextureResource texture, Rect rect) {
		this.texture = texture;
		this.rect = rect;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		graphics.blit(texture.location, rect.point.x, rect.point.y, 0, texture.offset.width, texture.offset.height, rect.width, rect.height, texture.fileSize.width, texture.fileSize.height);
	}


}
