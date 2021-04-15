package gollorum.signpost.minecraft.events;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.utils.WaystoneLocationData;

import java.util.Optional;

public class WaystoneAddedOrRemovedEvent extends WaystoneUpdatedEvent {

	public final Optional<PlayerHandle> owner;

	public WaystoneAddedOrRemovedEvent(
		WaystoneLocationData location,
		String name,
		Optional<PlayerHandle> owner
	) {
		super(location, name);
		this.owner = owner;
	}

	@Override
	public Type getType() {
		return null;
	}
}
