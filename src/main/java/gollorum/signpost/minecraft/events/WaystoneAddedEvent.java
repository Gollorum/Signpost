package gollorum.signpost.minecraft.events;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.utils.WaystoneLocationData;

public class WaystoneAddedEvent extends WaystoneAddedOrRemovedEvent {

    public WaystoneAddedEvent(WaystoneLocationData location, String name, PlayerHandle playerHandle, boolean shouldLock) {
        super(location, name, playerHandle, shouldLock);
    }

    @Override
    public Type getType() { return Type.Added; }

}