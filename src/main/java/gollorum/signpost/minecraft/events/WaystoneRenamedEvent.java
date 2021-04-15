package gollorum.signpost.minecraft.events;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.utils.WaystoneLocationData;

import java.util.Optional;

public class WaystoneRenamedEvent extends WaystoneAddedOrRemovedEvent {

    public final String oldName;

    public WaystoneRenamedEvent(
	    WaystoneLocationData location,
	    String newName,
	    String oldName,
	    Optional<PlayerHandle> owner
    ) {
        super(location, newName, owner);
        this.oldName = oldName;
    }

    @Override
    public Type getType() { return Type.Renamed; }

}
