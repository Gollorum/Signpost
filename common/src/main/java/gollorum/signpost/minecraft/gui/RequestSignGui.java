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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class RequestSignGui implements PacketHandler.Event.ForClient<RequestSignGui.Package> {

	public record Package(PostTile.TilePartInfo tilePartInfo) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Package> STREAM_CODEC = StreamCodec.composite(
			PostTile.TilePartInfo.STREAM_CODEC, Package::tilePartInfo,
			Package::new
		);
	}

	@Override
	public StreamCodec<RegistryFriendlyByteBuf, Package> codec() {
		return Package.STREAM_CODEC;
	}

	@Override
	public Class<Package> getMessageClass() { return Package.class; }

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
			SignGui.display(Tuple._1(), (SignBlockPart) Tuple._2().blockPart(), Tuple._2().offset(), message.tilePartInfo);
		} else {
			Signpost.LOGGER.error("Tried to open sign gui, but something was missing.");
		}
	}

	public static class ForNewSign implements PacketHandler.Event.ForClient<ForNewSign.Package> {

		public record Package(WorldLocation loc, PostBlock.ModelType modelType, Vector3 localHitPos, ItemStack itemToDropOnBreak) {
			public Package(WorldLocation loc, PostBlock.ModelType modelType, Vector3 localHitPos, ItemStack itemToDropOnBreak) {
				this.loc = loc.withoutExplicitLevel();
				this.modelType = modelType;
				this.localHitPos = localHitPos;
				this.itemToDropOnBreak = itemToDropOnBreak;
			}
			public static final StreamCodec<RegistryFriendlyByteBuf, Package> STREAM_CODEC = StreamCodec.composite(
				WorldLocation.STREAM_CODEC, Package::loc,
				PostBlock.ModelType.STREAM_CODEC, Package::modelType,
				Vector3.STREAM_CODEC, Package::localHitPos,
				ItemStack.OPTIONAL_STREAM_CODEC, Package::itemToDropOnBreak,
				Package::new
			);
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, Package> codec() {
			return Package.STREAM_CODEC;
		}

		@Override
		public Class<Package> getMessageClass() { return Package.class; }

		@Override
		public void handle(
			Package message, PacketHandler.Context.Client context
		) {
			TileEntityUtils.delayUntilTileEntityExistsAt(
				message.loc, PostTile.class,
				tile -> SignGui.display(tile, message.modelType, message.localHitPos, Optional.of(message.itemToDropOnBreak)),
				100,
				true,
				Optional.of(() -> Signpost.LOGGER.error("Tried to open sign gui for a new block, but the tile was missing."))
			);
		}

	}

}
