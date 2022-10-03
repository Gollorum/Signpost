package gollorum.signpost.minecraft.gui;

import gollorum.signpost.Teleport;
import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.utils.*;
import gollorum.signpost.minecraft.gui.widgets.GuiItemRenderer;
import gollorum.signpost.minecraft.gui.widgets.ImageView;
import gollorum.signpost.minecraft.gui.widgets.TextDisplay;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.Either;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class ConfirmTeleportGui extends ExtendedScreen {

	public static class SignInfo {
		public final PostTile tile;
		public final SignBlockPart sign;
		public final PostTile.TilePartInfo tilePartInfo;
		public final Vector3 offset;

		public SignInfo(
			PostTile tile,
			SignBlockPart sign,
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

	private final Either<String, Teleport.RequestGui.Package.Info> data;
	private final Optional<SignInfo> signInfo;

	public ConfirmTeleportGui(
		Either<String, Teleport.RequestGui.Package.Info> data,
		Optional<SignInfo> signInfo
	) {
		super(Component.translatable(LangKeys.confirmTeleportGuiTitle));
		this.data = data;
		this.signInfo = signInfo;
	}

	public static void display(
		Either<String, Teleport.RequestGui.Package.Info> data,
		Optional<SignInfo> signInfo
	) {
		Minecraft.getInstance().setScreen(new ConfirmTeleportGui(data, signInfo));
	}

	@Override
	protected void init() {
		super.init();
		AtomicInteger editButtonTop = new AtomicInteger();
		data.consume(
			langKey -> {
				addRenderableOnly(new TextDisplay(
					Component.translatable(langKey),
					new Point(width / 2, height / 2 - 20),
					Rect.XAlignment.Center, Rect.YAlignment.Bottom,
					font
				));
				editButtonTop.set(height / 2 + 20);
			},
			d -> {
				boolean isTooFarAway = d.maxDistance > 0 && d.distance > d.maxDistance;
				if(d.cannotTeleportBecause.isEmpty() && !isTooFarAway) {
					addRenderableOnly(new TextDisplay(
						Component.translatable(LangKeys.confirmTeleport, Colors.wrap(d.waystoneName, Colors.highlight)),
						new Point(width / 2, height / 2 - 20),
						Rect.XAlignment.Center, Rect.YAlignment.Bottom,
						font
					));

					if (!d.cost.isEmpty()) {
						addRenderableOnly(new TextDisplay(
							Component.translatable(LangKeys.cost),
							new Point(width / 2 - costCenterSpace / 2, height / 2),
							Rect.XAlignment.Right, Rect.YAlignment.Center,
							font
						));
						Rect itemRect = new Rect(
							new Point(width / 2 + costCenterSpace / 2, height / 2),
							TextureResource.itemBackground.size,
							Rect.XAlignment.Left, Rect.YAlignment.Center
						);
						addRenderableOnly(new ImageView(
							TextureResource.itemBackground,
							itemRect
						));
						addRenderableOnly(new GuiItemRenderer(
							new Rect(itemRect.center(), 16, 16, Rect.XAlignment.Center, Rect.YAlignment.Center),
							d.cost
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
					addRenderableWidget(new Button(
						confirmRect.point.x, confirmRect.point.y, confirmRect.width, confirmRect.height,
						Component.translatable(LangKeys.proceed),
						b -> confirm()
					));
					addRenderableWidget(new Button(
						cancelRect.point.x, cancelRect.point.y, cancelRect.width, cancelRect.height,
						Component.translatable(LangKeys.cancel),
						b -> cancel()
					));
					editButtonTop.set(cancelRect.max().y + 20);
				} else {
					d.cannotTeleportBecause.ifPresent(reason ->
						addRenderableOnly(new TextDisplay(
							reason,
							new Point(width / 2, height / 2 - 20),
							Rect.XAlignment.Center, Rect.YAlignment.Bottom,
							font
						)));
					if(isTooFarAway)
						addRenderableOnly(new TextDisplay(
							Component.translatable(
								LangKeys.tooFarAway,
								Colors.wrap(Integer.toString(d.distance), Colors.highlight),
								Colors.wrap(Integer.toString(d.maxDistance), Colors.highlight)
							),
							new Point(width / 2, height / 2 - (d.cannotTeleportBecause.isEmpty() ? 20 : 40)),
							Rect.XAlignment.Center, Rect.YAlignment.Bottom,
							font
						));
					editButtonTop.set(height / 2 + 20);
				}
			}
		);
		signInfo.ifPresent(info -> {
			Rect editRect = new Rect(new Point(width / 2, editButtonTop.get()), TextureResource.edit.size, Rect.XAlignment.Center, Rect.YAlignment.Top);
			if(info.sign.hasThePermissionToEdit(info.tile, getMinecraft().player)) {
				addRenderableWidget(new ImageButton(
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
		getMinecraft().setScreen(null);
		data.consume(
			langKey -> getMinecraft().player.displayClientMessage(Component.translatable(langKey), true),
			data -> PacketHandler.sendToServer(new Teleport.Request.Package(data.waystoneName, data.handle))
		);
	}

	private void cancel() {
		getMinecraft().setScreen(null);
	}

}
