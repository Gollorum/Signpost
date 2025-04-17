package gollorum.signpost.minecraft.gui;

import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.Tuple;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.ItemStackSerializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class RequestSignGui implements PacketHandler.Event.ForClient<RequestSignGui.Package> {

	public static class Package {
		public final PostTile.TilePartInfo tilePartInfo;

		public Package(PostTile.TilePartInfo tilePartInfo) {
			this.tilePartInfo = tilePartInfo;
		}
	}

	@Override
	public Class<Package> getMessageClass() { return Package.class; }

	@Override
	public void encode(RegistryFriendlyByteBuf buffer, Package message) {
		PostTile.TilePartInfo.BufferSerializer.encode(buffer, message.tilePartInfo);
	}

	@Override
	public Package decode(RegistryFriendlyByteBuf buffer) {
		return new Package(PostTile.TilePartInfo.BufferSerializer.decode(buffer));
	}

	@Override
	public void handle(
		Package message, PacketHandler.Context.Client context
	) {
		Optional<Tuple<PostTile, BlockPartInstance>> TupleO = TileEntityUtils.findTileEntityClient(
			message.tilePartInfo.dimensionKey, message.tilePartInfo.pos, PostTile.getBlockEntityType()
		).flatMap(tile -> tile.getPart(message.tilePartInfo.identifier)
			.flatMap(part -> (part.blockPart() instanceof SignBlockPart ? Optional.of(new Tuple<>(tile, part)) : Optional.empty())));
		if (TupleO.isPresent()) {
			Tuple<PostTile, BlockPartInstance> Tuple = TupleO.get();
			SignGui.display(Tuple._1, (SignBlockPart) Tuple._2.blockPart(), Tuple._2.offset(), message.tilePartInfo);
		} else {
			Signpost.LOGGER.error("Tried to open sign gui, but something was missing.");
		}
	}

	public static class ForNewSign implements PacketHandler.Event.ForClient<ForNewSign.Package> {

		public static class Package {
			private final WorldLocation loc;
			private final PostBlock.ModelType modelType;
			private final Vector3 localHitPos;
			private final ItemStack itemToDropOnBreak;

			public Package(WorldLocation loc, PostBlock.ModelType modelType, Vector3 localHitPos, ItemStack itemToDropOnBreak) {
				this.loc = loc.withoutExplicitLevel();
				this.modelType = modelType;
				this.localHitPos = localHitPos;
				this.itemToDropOnBreak = itemToDropOnBreak;
			}
		}

		@Override
		public Class<Package> getMessageClass() { return Package.class; }

		@Override
		public void encode(RegistryFriendlyByteBuf buffer, Package message) {
			WorldLocation.BUFFER_SERIALIZER.encode(buffer, message.loc);
			PostBlock.ModelType.Serializer.encode(buffer, message.modelType);
			Vector3.BufferSerializer.encode(buffer, message.localHitPos);
			ItemStackSerializer.Buffer.encode(buffer, message.itemToDropOnBreak);
		}

		@Override
		public Package decode(RegistryFriendlyByteBuf buffer) {
			return new Package(
				WorldLocation.BUFFER_SERIALIZER.decode(buffer),
				PostBlock.ModelType.Serializer.decode(buffer),
				Vector3.BufferSerializer.decode(buffer),
				ItemStackSerializer.Buffer.decode(buffer)
			);
		}

		@Override
		public void handle(
			Package message, PacketHandler.Context.Client context
		) {
			TileEntityUtils.delayUntilTileEntityExistsAt(
				message.loc, PostTile.class,
				tile -> SignGui.display(tile, message.modelType, message.localHitPos, message.itemToDropOnBreak),
				100,
				true,
				Optional.of(() -> Signpost.LOGGER.error("Tried to open sign gui for a new block, but the tile was missing."))
			);
		}

	}

}
