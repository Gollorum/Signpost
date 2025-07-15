package gollorum.signpost.minecraft.gui;

import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.WaystoneData;
import gollorum.signpost.utils.WorldLocation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

public class RequestWaystoneGui implements PacketHandler.Event.ForClient<RequestWaystoneGui.Package> {

	public record Package(WorldLocation location, Optional<WaystoneData> oldData) {
		public Package(WorldLocation location, Optional<WaystoneData> oldData) {
			this.location = location.withoutExplicitLevel();
			this.oldData = oldData.map(WaystoneData::withoutExplicitLevel);
		}
		public static final StreamCodec<RegistryFriendlyByteBuf, Package> STREAM_CODEC = StreamCodec.composite(
			WorldLocation.STREAM_CODEC, Package::location,
			ByteBufCodecs.optional(WaystoneData.STREAM_CODEC), Package::oldData,
			Package::new
		);
	}

	@Override
	public StreamCodec<RegistryFriendlyByteBuf, Package> codec() {
		return Package.STREAM_CODEC;
	}

	@Override
	public Class<RequestWaystoneGui.Package> getMessageClass() { return RequestWaystoneGui.Package.class; }

	@Override
	public void handle(
		RequestWaystoneGui.Package message, PacketHandler.Context.Client context
	) {
		WaystoneGui.display(message.location, message.oldData);
	}

}
