package gollorum.signpost.minecraft.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.Teleport;
import gollorum.signpost.networking.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.*;

public class ConfirmTeleportGui extends Screen {

	private static final TextureSize buttonsSize = new TextureSize(98, 20);
	private static final int costCenterSpace = 10;

	private final String waystoneName;
	private final List<IRenderable> additionallyRenderables = new ArrayList<>();
	private final ItemStack cost;

	public ConfirmTeleportGui(String waystoneName, ItemStack cost) {
		super(new TranslationTextComponent(LangKeys.confirmTeleportGuiTitle));
		this.waystoneName = waystoneName;
		this.cost = cost;
	}

	public static void display(String waystoneName, ItemStack cost) {
		Minecraft.getInstance().displayGuiScreen(new ConfirmTeleportGui(waystoneName, cost));
	}

	@Override
	protected void init() {
		super.init();
		additionallyRenderables.add(new TextDisplay(
			new TranslationTextComponent(LangKeys.confirmTeleport, waystoneName).getString(),
			new Point(width / 2, height / 2 - 20),
			Rect.XAlignment.Center, Rect.YAlignment.Bottom,
			font
		));

		if(!cost.isEmpty()) {
			additionallyRenderables.add(new TextDisplay(
				new TranslationTextComponent(LangKeys.cost).getString(),
				new Point(width / 2 - costCenterSpace / 2, height / 2),
				Rect.XAlignment.Right, Rect.YAlignment.Center,
				font
			));
			Rect itemRect = new Rect(
				new Point(width / 2 + costCenterSpace / 2, height / 2),
				TextureResource.itemBackground.size,
				Rect.XAlignment.Left, Rect.YAlignment.Center
			);
			additionallyRenderables.add(new ImageView(
				TextureResource.itemBackground,
				itemRect
			));
			additionallyRenderables.add(new GuiItemRenderer(
				new Rect(itemRect.center(), 16, 16, Rect.XAlignment.Center, Rect.YAlignment.Center),
				cost
			));
		}

		Rect confirmRect = new Rect(new Point(width / 2 + 20, height / 2 + 20), buttonsSize, Rect.XAlignment.Left, Rect.YAlignment.Top);
		Rect cancelRect = new Rect(new Point(width / 2 - 20, height / 2 + 20), buttonsSize, Rect.XAlignment.Right, Rect.YAlignment.Top);
		addButton(new Button(
			confirmRect.point.x, confirmRect.point.y, confirmRect.width, confirmRect.height,
			new TranslationTextComponent(LangKeys.proceed),
			b -> confirm()
		));
		addButton(new Button(
			cancelRect.point.x, cancelRect.point.y, cancelRect.width, cancelRect.height,
			new TranslationTextComponent(LangKeys.cancel),
			b -> cancel()
		));
	}

	private void confirm() {
		getMinecraft().displayGuiScreen(null);
		PacketHandler.sendToServer(new Teleport.Request.Package(waystoneName, cost));
	}

	private void cancel() {
		getMinecraft().displayGuiScreen(null);
	}

	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		renderBackground(matrixStack);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		for(IRenderable toRender : additionallyRenderables) {
			toRender.render(matrixStack, mouseX, mouseY, partialTicks);
		}
	}
}
