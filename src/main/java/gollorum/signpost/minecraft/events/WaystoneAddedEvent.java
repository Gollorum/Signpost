package gollorum.signpost.minecraft.events;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.utils.WaystoneLocationData;

public class WaystoneAddedEvent extends WaystoneUpdatedEvent {

    public WaystoneAddedEvent(WaystoneLocationData location, String name, PlayerHandle playerHandle) { super(location, name,
        playerHandle
    ); }

    @Override
    public Type getType() { return Type.Added; }

}