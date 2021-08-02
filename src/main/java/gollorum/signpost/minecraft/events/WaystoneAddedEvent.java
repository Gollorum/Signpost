package gollorum.signpost.minecraft.events;

import gollorum.signpost.utils.WaystoneLocationData;

public class WaystoneAddedEvent extends WaystoneAddedOrRenamedEvent {

    public WaystoneAddedEvent(WaystoneLocationData location, String name, boolean isLocked) {
        super(location, name, isLocked);
    }

    @Override
    public Type getType() { return Type.Added; }

}