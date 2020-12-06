package gollorum.signpost.minecraft.events;

import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.serialization.BufferSerializable;
import net.minecraft.network.PacketBuffer;

import java.util.Optional;

public abstract class WaystoneUpdatedEvent {

    public static WaystoneUpdatedEvent fromUpdated(WaystoneLocationData location, String name, Optional<String> oldName) {
        return oldName.map(n -> (WaystoneUpdatedEvent) new WaystoneRenamedEvent(location, name, n))
            .orElse(new WaystoneAddedEvent(location, name));
    }

    public static WaystoneUpdatedEvent fromUpdated(WaystoneLocationData location, String name) {
        return new WaystoneAddedEvent(location, name);
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
        public void writeTo(WaystoneUpdatedEvent event, PacketBuffer buffer) {
            buffer.writeEnumValue(event.getType());
            WaystoneLocationData.SERIALIZER.writeTo(event.location, buffer);
            buffer.writeString(event.name);
            if(event instanceof WaystoneRenamedEvent)
                buffer.writeString(((WaystoneRenamedEvent)event).oldName);
            else if(event instanceof WaystoneMovedEvent)
                WorldLocation.SERIALIZER.writeTo(((WaystoneMovedEvent)event).newLocation, buffer);
        }

        @Override
        public WaystoneUpdatedEvent readFrom(PacketBuffer buffer) {
            Type type = buffer.readEnumValue(Type.class);
            WaystoneLocationData location = WaystoneLocationData.SERIALIZER.readFrom(buffer);
            String name = buffer.readString();
            switch (type){
                case Added: return new WaystoneAddedEvent(location, name);
                case Removed: return new WaystoneRemovedEvent(location, name);
                case Renamed: return new WaystoneRenamedEvent(location, name, buffer.readString());
                case Moved: return new WaystoneMovedEvent(location, WorldLocation.SERIALIZER.readFrom(buffer), name);
                default: throw new RuntimeException("Type " + type + " is not supported");
            }
        }

    }

}