package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.gui.utils.TextureResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.StringTextComponent;

public class ImageView extends Widget {

	private final TextureResource texture;
	private final Rect rect;

	public ImageView(TextureResource texture, Rect rect) {
		super(rect.point.x, rect.point.y, rect.width, rect.height, new StringTextComponent("Image View"));
		this.texture = texture;
		this.rect = rect;
	}

	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		this.setBlitOffset(0);
		Minecraft.getInstance().getTextureManager().bind(texture.location);
		blit(matrixStack, x, y, texture.offset.width, texture.offset.height, width, height, texture.fileSize.width, texture.fileSize.height);
		this.setBlitOffset(0);
	}


}