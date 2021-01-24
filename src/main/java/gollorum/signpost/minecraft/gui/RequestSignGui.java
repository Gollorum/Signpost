package gollorum.signpost.minecraft.gui;

import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.types.Sign;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.TileEntityUtils;
import javafx.util.Pair;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class RequestSignGui implements PacketHandler.Event<RequestSignGui.Package> {

	public static class Package {
		public final PostTile.TilePartInfo tilePartInfo;

		public Package(PostTile.TilePartInfo tilePartInfo) {
			this.tilePartInfo = tilePartInfo;
		}
	}

	@Override
	public Class<Package> getMessageClass() { return Package.class; }

	@Override
	public void encode(Package message, PacketBuffer buffer) {
		PostTile.TilePartInfo.SERIALIZER.writeTo(message.tilePartInfo, buffer);
	}

	@Override
	public Package decode(PacketBuffer buffer) {
		return new Package(PostTile.TilePartInfo.SERIALIZER.readFrom(buffer));
	}

	@Override
	public void handle(
		Package message, Supplier<NetworkEvent.Context> context
	) {
		Optional<Pair<PostTile, BlockPartInstance>> pairO = TileEntityUtils.findTileEntityClient(
			message.tilePartInfo.dimensionKey, message.tilePartInfo.pos, PostTile.class
		).flatMap(tile -> tile.getPart(message.tilePartInfo.identifier)
			.flatMap(part -> (part.blockPart instanceof Sign ? Optional.of(new Pair<>(tile, part)) : Optional.empty())));
		if(pairO.isPresent()) {
			Pair<PostTile, BlockPartInstance> pair = pairO.get();
			SignGui.display(pair.getKey(), (Sign) pair.getValue().blockPart, pair.getValue().offset, message.tilePartInfo);
		} else {
			Signpost.LOGGER.error("Tried to open sign gui, but something was missing.");
		}
	}

}
