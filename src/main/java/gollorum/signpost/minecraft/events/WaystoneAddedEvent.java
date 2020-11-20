package gollorum.signpost.minecraft.events;

import gollorum.signpost.utils.WaystoneLocationData;

public class WaystoneAddedEvent extends WaystoneUpdatedEvent {

    public WaystoneAddedEvent(WaystoneLocationData location, String name) { super(location, name); }

    @Override
    public Type getType() { return Type.Added; }

}