package gollorum.signpost.minecraft.events;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.StringSerializer;
import net.minecraft.network.PacketBuffer;

import java.util.Optional;
import java.util.UUID;

public abstract class WaystoneUpdatedEvent {

    public static WaystoneUpdatedEvent fromUpdated(
        WaystoneLocationData location, String name, Optional<String> oldName, boolean isLocked, WaystoneHandle.Vanilla handle
    ) {
        return oldName.map(n -> (WaystoneUpdatedEvent) new WaystoneRenamedEvent(location, name, n, isLocked, handle))
            .orElse(new WaystoneAddedEvent(location, name, isLocked, handle));
    }

    public static WaystoneUpdatedEvent fromUpdated(
        WaystoneLocationData location, String name, boolean isLocked, WaystoneHandle.Vanilla handle
    ) {
        return new WaystoneAddedEvent(location, name, isLocked, handle);
    }

    public enum Type {
        Added, Removed, Renamed, Moved
    }

    public final WaystoneLocationData location;
    public final String name;
    public final WaystoneHandle.Vanilla handle;

    public WaystoneUpdatedEvent(WaystoneLocationData location, String name, WaystoneHandle.Vanilla handle) {
        this.location = location;
        this.name = name;
        this.handle = handle;
    }

    public abstract Type getType();

    public static class Serializer implements BufferSerializable<WaystoneUpdatedEvent> {

        public static final Serializer INSTANCE = new Serializer();
        private Serializer(){}

        @Override
        public Class<WaystoneUpdatedEvent> getTargetClass() {
            return WaystoneUpdatedEvent.class;
        }

        public void write(WaystoneUpdatedEvent event, PacketBuffer buffer) {
            buffer.writeEnum(event.getType());
            WaystoneLocationData.SERIALIZER.write(event.location, buffer);
            StringSerializer.instance.write(event.name, buffer);
            WaystoneHandle.Vanilla.Serializer.write(event.handle, buffer);
            if(event instanceof WaystoneRenamedEvent)
                StringSerializer.instance.write(((WaystoneRenamedEvent)event).oldName, buffer);
            else if(event instanceof WaystoneMovedEvent)
                WorldLocation.SERIALIZER.write(((WaystoneMovedEvent)event).newLocation, buffer);
            if(event instanceof WaystoneAddedOrRenamedEvent)
                buffer.writeBoolean(((WaystoneAddedOrRenamedEvent) event).isLocked);
        }

        public WaystoneUpdatedEvent read(PacketBuffer buffer) {
            Type type = buffer.readEnum(Type.class);
            WaystoneLocationData location = WaystoneLocationData.SERIALIZER.read(buffer);
            String name = StringSerializer.instance.read(buffer);
            WaystoneHandle.Vanilla handle = WaystoneHandle.Vanilla.Serializer.read(buffer);
            switch (type){
                case Added: return new WaystoneAddedEvent(location, name, buffer.readBoolean(), handle);
                case Removed: return new WaystoneRemovedEvent(location, name, handle);
                case Renamed: return new WaystoneRenamedEvent(location, name, StringSerializer.instance.read(buffer), buffer.readBoolean(), handle);
                case Moved: return new WaystoneMovedEvent(location, WorldLocation.SERIALIZER.read(buffer), name, handle);
                default: throw new RuntimeException("Type " + type + " is not supported");
            }
        }

    }

}