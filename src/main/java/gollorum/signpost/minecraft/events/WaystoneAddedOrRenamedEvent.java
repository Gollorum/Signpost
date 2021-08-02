package gollorum.signpost.minecraft.events;

import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.OwnershipData;

public class WaystoneAddedOrRenamedEvent extends WaystoneUpdatedEvent {

	public final boolean isLocked;

	public WaystoneAddedOrRenamedEvent(
		WaystoneLocationData location,
		String name,
		boolean isLocked
	) {
		super(location, name);
		this.isLocked = isLocked;
	}

	@Override
	public Type getType() {
		return null;
	}
}
