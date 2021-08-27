package gollorum.signpost.minecraft.events;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.utils.WaystoneLocationData;

public class WaystoneAddedOrRenamedEvent extends WaystoneUpdatedEvent {

	public final boolean isLocked;

	public WaystoneAddedOrRenamedEvent(
		WaystoneLocationData location,
		String name,
		boolean isLocked,
		WaystoneHandle.Vanilla id
	) {
		super(location, name, id);
		this.isLocked = isLocked;
	}

	@Override
	public Type getType() {
		return null;
	}
}
