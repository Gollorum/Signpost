package gollorum.signpost.minecraft.events;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.utils.WaystoneLocationData;

public class WaystoneRemovedEvent extends WaystoneUpdatedEvent {

    public WaystoneRemovedEvent(WaystoneLocationData location, String name, PlayerHandle playerHandle) {
        super(location, name, playerHandle);
    }

    @Override
    public Type getType() { return Type.Removed; }

}