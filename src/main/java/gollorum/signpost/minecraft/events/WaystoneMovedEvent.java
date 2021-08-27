package gollorum.signpost.minecraft.events;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;

public class WaystoneMovedEvent extends WaystoneUpdatedEvent {

    public final WorldLocation newLocation;

    public WaystoneMovedEvent(WaystoneLocationData oldLocation, WorldLocation newLocation, String name, WaystoneHandle.Vanilla id) {
        super(oldLocation, name, id);
        this.newLocation = newLocation;
    }

    @Override
    public Type getType() { return Type.Moved; }
}
