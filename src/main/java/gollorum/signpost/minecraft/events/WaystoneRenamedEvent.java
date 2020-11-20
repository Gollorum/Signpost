package gollorum.signpost.minecraft.events;

import gollorum.signpost.utils.WaystoneLocationData;

public class WaystoneRenamedEvent extends WaystoneUpdatedEvent {

    public final String oldName;

    public WaystoneRenamedEvent(WaystoneLocationData location, String newName, String oldName) {
        super(location, newName);
        this.oldName = oldName;
    }

    @Override
    public Type getType() { return Type.Renamed; }

}
