package gollorum.signpost.minecraft.events;

import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.OwnershipData;

public class WaystoneRenamedEvent extends WaystoneAddedOrRemovedEvent {

    public final String oldName;

    public WaystoneRenamedEvent(
	    WaystoneLocationData location,
	    String newName,
	    String oldName,
	    OwnershipData ownership
    ) {
        super(location, newName, ownership);
        this.oldName = oldName;
    }

    @Override
    public Type getType() { return Type.Renamed; }

}
