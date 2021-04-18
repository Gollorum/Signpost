package gollorum.signpost.minecraft.events;

import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.OwnershipData;

public class WaystoneAddedEvent extends WaystoneAddedOrRemovedEvent {

    public WaystoneAddedEvent(WaystoneLocationData location, String name, OwnershipData ownership) {
        super(location, name, ownership);
    }

    @Override
    public Type getType() { return Type.Added; }

}