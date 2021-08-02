package gollorum.signpost.minecraft.events;

import gollorum.signpost.utils.WaystoneLocationData;

public class WaystoneRenamedEvent extends WaystoneAddedOrRenamedEvent {

    public final String oldName;

    public WaystoneRenamedEvent(
	    WaystoneLocationData location,
	    String newName,
	    String oldName,
	    boolean isLocked
    ) {
        super(location, newName, isLocked);
        this.oldName = oldName;
    }

    @Override
    public Type getType() { return Type.Renamed; }

}
