package gollorum.signpost.minecraft.events;

import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.OwnershipData;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.serialization.BufferSerializable;
import net.minecraft.network.PacketBuffer;

import java.util.Optional;

public abstract class WaystoneUpdatedEvent {

    public static WaystoneUpdatedEvent fromUpdated(
        WaystoneLocationData location, String name, Optional<String> oldName, boolean isLocked
    ) {
        return oldName.map(n -> (WaystoneUpdatedEvent) new WaystoneRenamedEvent(location, name, n, isLocked))
            .orElse(new WaystoneAddedEvent(location, name, isLocked));
    }

    public static WaystoneUpdatedEvent fromUpdated(
        WaystoneLocationData location, String name, boolean isLocked
    ) {
        return new WaystoneAddedEvent(location, name, isLocked);
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
            if(event instanceof WaystoneAddedOrRenamedEvent)
                buffer.writeBoolean(((WaystoneAddedOrRenamedEvent) event).isLocked);
        }

        @Override
        public WaystoneUpdatedEvent read(PacketBuffer buffer) {
            Type type = buffer.readEnumValue(Type.class);
            WaystoneLocationData location = WaystoneLocationData.SERIALIZER.read(buffer);
            String name = buffer.readString(32767);
            switch (type){
                case Added: return new WaystoneAddedEvent(location, name, buffer.readBoolean());
                case Removed: return new WaystoneRemovedEvent(location, name);
                case Renamed: return new WaystoneRenamedEvent(location, name, buffer.readString(32767), buffer.readBoolean());
                case Moved: return new WaystoneMovedEvent(location, WorldLocation.SERIALIZER.read(buffer), name);
                default: throw new RuntimeException("Type " + type + " is not supported");
            }
        }

    }

}