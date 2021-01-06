package gollorum.signpost.minecraft.events;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;

public class WaystoneMovedEvent extends WaystoneUpdatedEvent {

    public final WorldLocation newLocation;

    public WaystoneMovedEvent(WaystoneLocationData oldLocation, WorldLocation newLocation, String name, PlayerHandle playerHandle) {
        super(oldLocation, name, playerHandle);
        this.newLocation = newLocation;
    }

    @Override
    public Type getType() { return Type.Moved; }
}
