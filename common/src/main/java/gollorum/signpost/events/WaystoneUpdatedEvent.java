package gollorum.signpost.events;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

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
        this.location = location.withoutExplicitLevel();
        this.name = name;
        this.handle = handle;
    }

    public abstract Type getType();

    public static class Serializer implements StreamCodec<FriendlyByteBuf, WaystoneUpdatedEvent> {

        public static final Serializer INSTANCE = new Serializer();
        private Serializer(){}

        @Override
        public void encode(FriendlyByteBuf buffer, WaystoneUpdatedEvent event) {
            buffer.writeEnum(event.getType());
            WaystoneLocationData.STREAM_CODEC.encode(buffer, event.location);
            ByteBufCodecs.STRING_UTF8.encode(buffer, event.name);
            WaystoneHandle.Vanilla.STREAM_CODEC.encode(buffer, event.handle);
            if(event instanceof WaystoneRenamedEvent)
                ByteBufCodecs.STRING_UTF8.encode(buffer, ((WaystoneRenamedEvent)event).oldName);
            else if(event instanceof WaystoneMovedEvent)
                WorldLocation.STREAM_CODEC.encode(buffer, ((WaystoneMovedEvent)event).newLocation);
            if(event instanceof WaystoneAddedOrRenamedEvent)
                buffer.writeBoolean(((WaystoneAddedOrRenamedEvent) event).isLocked);
        }

        @Override
        public WaystoneUpdatedEvent decode(FriendlyByteBuf buffer) {
            Type type = buffer.readEnum(Type.class);
            WaystoneLocationData location = WaystoneLocationData.STREAM_CODEC.decode(buffer);
            String name = ByteBufCodecs.STRING_UTF8.decode(buffer);
            WaystoneHandle.Vanilla handle = WaystoneHandle.Vanilla.STREAM_CODEC.decode(buffer);
            return switch (type) {
                case Added -> new WaystoneAddedEvent(location, name, buffer.readBoolean(), handle);
                case Removed -> new WaystoneRemovedEvent(location, name, handle);
                case Renamed -> new WaystoneRenamedEvent(location, name, ByteBufCodecs.STRING_UTF8.decode(buffer), buffer.readBoolean(), handle);
                case Moved -> new WaystoneMovedEvent(location, WorldLocation.STREAM_CODEC.decode(buffer), name, handle);
                default -> throw new RuntimeException("Type " + type + " is not supported");
            };
        }

    }

}