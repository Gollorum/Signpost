package gollorum.signpost.minecraft.gui;

import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.WaystoneData;
import gollorum.signpost.utils.WorldLocation;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.Optional;

public class RequestWaystoneGui implements PacketHandler.Event.ForClient<RequestWaystoneGui.Package> {

	public static class Package {
		public final WorldLocation location;
		public final Optional<WaystoneData> oldData;

		public Package(WorldLocation location, Optional<WaystoneData> oldData) {
			this.location = location.withoutExplicitLevel();
            this.oldData = oldData.map(WaystoneData::withoutExplicitLevel);
		}
	}

	@Override
	public Class<RequestWaystoneGui.Package> getMessageClass() { return RequestWaystoneGui.Package.class; }

	@Override
	public void encode(RegistryFriendlyByteBuf buffer, Package message) {
		WorldLocation.BUFFER_SERIALIZER.encode(buffer, message.location);
		WaystoneData.BUFFER_SERIALIZER.optional().encode(buffer, message.oldData);
	}

	@Override
	public RequestWaystoneGui.Package decode(RegistryFriendlyByteBuf buffer) {
		return new RequestWaystoneGui.Package(
			WorldLocation.BUFFER_SERIALIZER.decode(buffer),
			WaystoneData.BUFFER_SERIALIZER.optional().decode(buffer)
		);
	}

	@Override
	public void handle(
		RequestWaystoneGui.Package message, PacketHandler.Context.Client context
	) {
		WaystoneGui.display(message.location, message.oldData);
	}

}
