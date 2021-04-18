package gollorum.signpost.minecraft.events;

import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.OwnershipData;

public class WaystoneAddedOrRemovedEvent extends WaystoneUpdatedEvent {

	public final OwnershipData ownership;

	public WaystoneAddedOrRemovedEvent(
		WaystoneLocationData location,
		String name,
		OwnershipData ownership
	) {
		super(location, name);
		this.ownership = ownership;
	}

	@Override
	public Type getType() {
		return null;
	}
}
