package gollorum.signpost.minecraft.events;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.utils.WaystoneLocationData;

public class WaystoneAddedEvent extends WaystoneAddedOrRenamedEvent {

    public WaystoneAddedEvent(WaystoneLocationData location, String name, boolean isLocked, WaystoneHandle.Vanilla id) {
        super(location, name, isLocked, id);
    }

    @Override
    public Type getType() { return Type.Added; }

}