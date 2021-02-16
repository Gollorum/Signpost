package gollorum.signpost.minecraft.events;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.utils.WaystoneLocationData;

public class WaystoneAddedOrRemovedEvent extends WaystoneUpdatedEvent {

	public final boolean shouldLock;

	public WaystoneAddedOrRemovedEvent(
		WaystoneLocationData location,
		String name,
		PlayerHandle playerHandle,
		boolean shouldLock
	) {
		super(location, name, playerHandle);
		this.shouldLock = shouldLock;
	}

	@Override
	public Type getType() {
		return null;
	}
}
