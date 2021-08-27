package gollorum.signpost.minecraft.events;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.utils.WaystoneLocationData;

public class WaystoneRenamedEvent extends WaystoneAddedOrRenamedEvent {

    public final String oldName;

    public WaystoneRenamedEvent(
	    WaystoneLocationData location,
	    String newName,
	    String oldName,
	    boolean isLocked,
        WaystoneHandle.Vanilla id
    ) {
        super(location, newName, isLocked, id);
        this.oldName = oldName;
    }

    @Override
    public Type getType() { return Type.Renamed; }

}
