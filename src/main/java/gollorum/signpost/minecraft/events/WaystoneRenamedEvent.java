package gollorum.signpost.minecraft.events;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.utils.WaystoneLocationData;

public class WaystoneRenamedEvent extends WaystoneAddedOrRemovedEvent {

    public final String oldName;

    public WaystoneRenamedEvent(
	    WaystoneLocationData location,
	    String newName,
	    String oldName,
	    PlayerHandle playerHandle,
	    boolean shouldLock
    ) {
        super(location, newName, playerHandle, shouldLock);
        this.oldName = oldName;
    }

    @Override
    public Type getType() { return Type.Renamed; }

}
