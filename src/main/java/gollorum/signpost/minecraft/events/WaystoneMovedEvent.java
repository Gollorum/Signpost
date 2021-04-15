package gollorum.signpost.minecraft.events;

import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;

public class WaystoneMovedEvent extends WaystoneUpdatedEvent {

    public final WorldLocation newLocation;

    public WaystoneMovedEvent(WaystoneLocationData oldLocation, WorldLocation newLocation, String name) {
        super(oldLocation, name);
        this.newLocation = newLocation;
    }

    @Override
    public Type getType() { return Type.Moved; }
}
