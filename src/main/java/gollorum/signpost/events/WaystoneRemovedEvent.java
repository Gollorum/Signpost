package gollorum.signpost.events;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.utils.WaystoneLocationData;

public class WaystoneRemovedEvent extends WaystoneUpdatedEvent {

    public WaystoneRemovedEvent(WaystoneLocationData location, String name, WaystoneHandle.Vanilla id) {
        super(location, name, id);
    }

    @Override
    public Type getType() { return Type.Removed; }

}