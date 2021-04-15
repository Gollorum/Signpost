package gollorum.signpost.minecraft.events;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.OptionalSerializer;
import net.minecraft.network.PacketBuffer;

import java.util.Optional;

public abstract class WaystoneUpdatedEvent {

    public static WaystoneUpdatedEvent fromUpdated(
        WaystoneLocationData location, String name, Optional<String> oldName, Optional<PlayerHandle> owner
    ) {
        return oldName.map(n -> (WaystoneUpdatedEvent) new WaystoneRenamedEvent(location, name, n, owner))
            .orElse(new WaystoneAddedEvent(location, name, owner));
    }

    public static WaystoneUpdatedEvent fromUpdated(
        WaystoneLocationData location, String name, Optional<PlayerHandle> owner
    ) {
        return new WaystoneAddedEvent(location, name, owner);
    }

    public enum Type {
        Added, Removed, Renamed, Moved
    }

    public final WaystoneLocationData location;
    public final String name;

    public WaystoneUpdatedEvent(WaystoneLocationData location, String name) {
        this.location = location;
        this.name = name;
    }

    public abstract Type getType();

    public static class Serializer implements BufferSerializable<WaystoneUpdatedEvent> {

        public static final Serializer INSTANCE = new Serializer();
        private static final CompoundSerializable<Optional<PlayerHandle>> ownerSerializer =
            OptionalSerializer.from(PlayerHandle.Serializer);
        private Serializer(){}

        @Override
        public void write(WaystoneUpdatedEvent event, PacketBuffer buffer) {
            buffer.writeEnumValue(event.getType());
            WaystoneLocationData.SERIALIZER.write(event.location, buffer);
            buffer.writeString(event.name);
            if(event instanceof WaystoneRenamedEvent)
                buffer.writeString(((WaystoneRenamedEvent)event).oldName);
            else if(event instanceof WaystoneMovedEvent)
                WorldLocation.SERIALIZER.write(((WaystoneMovedEvent)event).newLocation, buffer);
            if(event instanceof WaystoneAddedOrRemovedEvent)
                ownerSerializer.write(((WaystoneAddedOrRemovedEvent) event).owner, buffer);
        }

        @Override
        public WaystoneUpdatedEvent read(PacketBuffer buffer) {
            Type type = buffer.readEnumValue(Type.class);
            WaystoneLocationData location = WaystoneLocationData.SERIALIZER.read(buffer);
            String name = buffer.readString(32767);
            switch (type){
                case Added: return new WaystoneAddedEvent(location, name, ownerSerializer.read(buffer));
                case Removed: return new WaystoneRemovedEvent(location, name);
                case Renamed: return new WaystoneRenamedEvent(location, name, buffer.readString(32767), ownerSerializer.read(buffer));
                case Moved: return new WaystoneMovedEvent(location, WorldLocation.SERIALIZER.read(buffer), name);
                default: throw new RuntimeException("Type " + type + " is not supported");
            }
        }

    }

}