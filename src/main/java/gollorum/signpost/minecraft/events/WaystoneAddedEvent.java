package gollorum.signpost.minecraft.events;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.utils.WaystoneLocationData;

import java.util.Optional;

public class WaystoneAddedEvent extends WaystoneAddedOrRemovedEvent {

    public WaystoneAddedEvent(WaystoneLocationData location, String name, Optional<PlayerHandle> owner) {
        super(location, name, owner);
    }

    @Override
    public Type getType() { return Type.Added; }

}