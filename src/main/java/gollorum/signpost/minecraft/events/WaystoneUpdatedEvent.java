package gollorum.signpost.minecraft.events;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.serialization.BufferSerializable;
import net.minecraft.network.PacketBuffer;

import java.util.Optional;

public abstract class WaystoneUpdatedEvent {

    public static WaystoneUpdatedEvent fromUpdated(
        WaystoneLocationData location, String name, Optional<String> oldName, PlayerHandle playerHandle, boolean shouldLock
    ) {
        return oldName.map(n -> (WaystoneUpdatedEvent) new WaystoneRenamedEvent(location, name, n, playerHandle, shouldLock))
            .orElse(new WaystoneAddedEvent(location, name,playerHandle, shouldLock));
    }

    public static WaystoneUpdatedEvent fromUpdated(
        WaystoneLocationData location, String name, PlayerHandle playerHandle, boolean shouldLock
    ) {
        return new WaystoneAddedEvent(location, name, playerHandle, shouldLock);
    }

    public enum Type {
        Added, Removed, Renamed, Moved
    }

    public final WaystoneLocationData location;
    public final String name;
    public final PlayerHandle playerHandle;

    public WaystoneUpdatedEvent(WaystoneLocationData location, String name, PlayerHandle playerHandle) {
        this.location = location;
        this.name = name;
        this.playerHandle = playerHandle;
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
            PlayerHandle.SERIALIZER.writeTo(event.playerHandle, buffer);
            if(event instanceof WaystoneRenamedEvent)
                buffer.writeString(((WaystoneRenamedEvent)event).oldName);
            else if(event instanceof WaystoneMovedEvent)
                WorldLocation.SERIALIZER.writeTo(((WaystoneMovedEvent)event).newLocation, buffer);
            if(event instanceof WaystoneAddedOrRemovedEvent)
                buffer.writeBoolean(((WaystoneAddedOrRemovedEvent) event).shouldLock);
        }

        @Override
        public WaystoneUpdatedEvent readFrom(PacketBuffer buffer) {
            Type type = buffer.readEnumValue(Type.class);
            WaystoneLocationData location = WaystoneLocationData.SERIALIZER.readFrom(buffer);
            String name = buffer.readString(32767);
            PlayerHandle playerHandle = PlayerHandle.SERIALIZER.readFrom(buffer);
            switch (type){
                case Added: return new WaystoneAddedEvent(location, name, playerHandle, buffer.readBoolean());
                case Removed: return new WaystoneRemovedEvent(location, name, playerHandle);
                case Renamed: return new WaystoneRenamedEvent(location, name, buffer.readString(32767), playerHandle, buffer.readBoolean());
                case Moved: return new WaystoneMovedEvent(location, WorldLocation.SERIALIZER.readFrom(buffer), name, playerHandle);
                default: throw new RuntimeException("Type " + type + " is not supported");
            }
        }

    }

}