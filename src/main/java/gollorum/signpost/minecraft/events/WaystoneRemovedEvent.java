package gollorum.signpost.minecraft.events;

import gollorum.signpost.utils.WaystoneLocationData;

public class WaystoneRemovedEvent extends WaystoneUpdatedEvent {

    public WaystoneRemovedEvent(WaystoneLocationData location, String name) { super(location, name); }

    @Override
    public Type getType() { return Type.Removed; }

}