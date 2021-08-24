package gollorum.signpost.minecraft.gui;

import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.WaystoneData;
import gollorum.signpost.utils.WorldLocation;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class RequestWaystoneGui implements PacketHandler.Event<RequestWaystoneGui.Package> {

	public static class Package {
		public final WorldLocation location;
		public final Optional<WaystoneData> oldData;

		public Package(WorldLocation location, Optional<WaystoneData> oldData) {
			this.location = location;
			this.oldData = oldData;
		}
	}

	@Override
	public Class<RequestWaystoneGui.Package> getMessageClass() { return RequestWaystoneGui.Package.class; }

	@Override
	public void encode(RequestWaystoneGui.Package message, PacketBuffer buffer) {
		WorldLocation.SERIALIZER.write(message.location, buffer);
		WaystoneData.SERIALIZER.optional().write(message.oldData, buffer);
	}

	@Override
	public RequestWaystoneGui.Package decode(PacketBuffer buffer) {
		return new RequestWaystoneGui.Package(
			WorldLocation.SERIALIZER.read(buffer),
			WaystoneData.SERIALIZER.optional().read(buffer)
		);
	}

	@Override
	public void handle(
		RequestWaystoneGui.Package message, NetworkEvent.Context context
	) {
		WaystoneGui.display(message.location, message.oldData);
	}

}
