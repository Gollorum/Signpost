package gollorum.signpost.minecraft.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.Teleport;
import gollorum.signpost.blockpartdata.types.Sign;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.gui.utils.*;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.math.geometry.Vector3;
import javafx.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.*;

public class ConfirmTeleportGui extends Screen {

	public static class SignInfo {
		public final PostTile tile;
		public final Sign sign;
		public final PostTile.TilePartInfo tilePartInfo;
		public final Vector3 offset;

		public SignInfo(
			PostTile tile,
			Sign sign,
			PostTile.TilePartInfo tilePartInfo,
			Vector3 offset
		) {
			this.tile = tile;
			this.sign = sign;
			this.tilePartInfo = tilePartInfo;
			this.offset = offset;
		}
	}

	private static final TextureSize buttonsSize = new TextureSize(98, 20);
	private static final int costCenterSpace = 10;

	private final String waystoneName;
	private final List<IRenderable> additionallyRenderables = new ArrayList<>();
	private final ItemStack cost;
	private final boolean isDiscovered;
	private final int distance;
	private final int maxDistance;
	private final Optional<SignInfo> signInfo;

	public ConfirmTeleportGui(
		String waystoneName,
		ItemStack cost,
		boolean isDiscovered,
		int distance,
		int maxDistance,
		Optional<SignInfo> signInfo
	) {
		super(new TranslationTextComponent(LangKeys.confirmTeleportGuiTitle));
		this.waystoneName = waystoneName;
		this.cost = cost;
		this.isDiscovered = isDiscovered;
		this.distance = distance;
		this.maxDistance = maxDistance;
		this.signInfo = signInfo;
	}

	public static void display(
		String waystoneName,
		ItemStack cost,
		boolean isDiscovered,
		int distance,
		int maxDistance,
		Optional<SignInfo> signInfo
	) {
		Minecraft.getInstance().displayGuiScreen(new ConfirmTeleportGui(waystoneName, cost, isDiscovered, distance, maxDistance, signInfo));
	}

	@Override
	protected void init() {
		super.init();
		int editButtonTop;
		boolean isTooFarAway = maxDistance > 0 && distance > maxDistance;
		if(isDiscovered && !isTooFarAway) {
			additionallyRenderables.add(new TextDisplay(
				new Point(width / 2, height / 2 - 20),
				Rect.XAlignment.Center, Rect.YAlignment.Bottom,
				font,
				LangKeys.confirmTeleport, new Pair<>(waystoneName, Colors.highlight)
			));

			if (!cost.isEmpty()) {
				additionallyRenderables.add(new TextDisplay(
					new Point(width / 2 - costCenterSpace / 2, height / 2),
					Rect.XAlignment.Right, Rect.YAlignment.Center,
					font,
					LangKeys.cost
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

			Rect confirmRect = new Rect(
				new Point(width / 2 + 20, height / 2 + 20),
				buttonsSize,
				Rect.XAlignment.Left,
				Rect.YAlignment.Top
			);
			Rect cancelRect = new Rect(
				new Point(width / 2 - 20, height / 2 + 20),
				buttonsSize,
				Rect.XAlignment.Right,
				Rect.YAlignment.Top
			);
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
			editButtonTop = cancelRect.max().y + 20;
		} else {
			if(!isDiscovered)
				additionallyRenderables.add(new TextDisplay(
					new Point(width / 2, height / 2 - 20),
					Rect.XAlignment.Center, Rect.YAlignment.Bottom,
					font,
					LangKeys.notDiscovered,
					new Pair<>(waystoneName, Colors.highlight)
				));
			if(isTooFarAway)
				additionallyRenderables.add(new TextDisplay(
					new Point(width / 2, height / 2 - (isDiscovered ? 20 : 40)),
					Rect.XAlignment.Center, Rect.YAlignment.Bottom,
					font,
					LangKeys.tooFarAway,
					new Pair<>(Integer.toString(distance), Colors.highlight),
					new Pair<>(Integer.toString(maxDistance), Colors.highlight)
				));
			editButtonTop = height / 2 + 20;
		}
		signInfo.ifPresent(info -> {
			Rect editRect = new Rect(new Point(width / 2, editButtonTop), TextureResource.edit.size, Rect.XAlignment.Center, Rect.YAlignment.Top);
			if(info.sign.hasThePermissionToEdit(info.tile, getMinecraft().player)) {
				addButton(new ImageButton(
					editRect.point.x, editRect.point.y,
					editRect.width, editRect.height,
					0, 0, TextureResource.edit.size.height,
					TextureResource.edit.location,
					TextureResource.edit.fileSize.width, TextureResource.edit.fileSize.height,
					b -> SignGui.display(info.tile, info.sign, info.offset, info.tilePartInfo)
				));
			}
		});
	}

	private void confirm() {
		getMinecraft().displayGuiScreen(null);
		PacketHandler.sendToServer(new Teleport.Request.Package(waystoneName, isDiscovered, distance, maxDistance, cost, signInfo.map(i -> i.tilePartInfo)));
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
