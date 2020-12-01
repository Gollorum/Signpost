package gollorum.signpost;

import gollorum.signpost.minecraft.events.WaystoneMovedEvent;
import gollorum.signpost.minecraft.events.WaystoneRemovedEvent;
import gollorum.signpost.minecraft.events.WaystoneUpdatedEvent;
import gollorum.signpost.minecraft.storage.WaystoneLibraryStorage;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.EventDispatcher;
import gollorum.signpost.utils.WaystoneData;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.OptionalSerializer;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class WaystoneLibrary {

    private static WaystoneLibrary instance;
    public static WaystoneLibrary getInstance() { return instance; }

    private WorldSavedData savedData;
    public boolean hasStorageBeenSetup(){ return savedData != null; }

    private EventDispatcher.Impl.WithPublicDispatch<WaystoneUpdatedEvent> _updateEventDispatcher = new EventDispatcher.Impl.WithPublicDispatch<>();

    public EventDispatcher<WaystoneUpdatedEvent> updateEventDispatcher = _updateEventDispatcher;

    public static void initialize() {
        instance = new WaystoneLibrary();
        PacketHandler.register(new RequestAllWaystoneNamesEvent());
        PacketHandler.register(new DeliverAllWaystoneNamesEvent());
        PacketHandler.register(new WaystoneUpdatedEventEvent());
    }

    public void setupStorage(ServerWorld world){
        DimensionSavedDataManager storage = world.getSavedData();
        savedData = storage.getOrCreate(WaystoneLibraryStorage::new, WaystoneLibraryStorage.NAME);
    }

    private WaystoneLibrary() { }

    public WaystoneLocationData getLocationData(UUID waystoneId) {
        return allWaystones.get(waystoneId).locationData;
    }

    private static class WaystoneEntry {
        public final String name;
        public final WaystoneLocationData locationData;
        public WaystoneEntry(String name, WaystoneLocationData locationData) {
            this.name = name;
            this.locationData = locationData;
        }
    }

    private final Map<UUID, WaystoneEntry> allWaystones = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> playerMemory = new ConcurrentHashMap<>();

    private final EventDispatcher.Impl.WithPublicDispatch<Map<UUID, String>> requestedAllNamesEventDispatcher =
        new EventDispatcher.Impl.WithPublicDispatch<>();

    private final EventDispatcher.Impl.WithPublicDispatch<Optional<UUID>> requestedIdEventDispatcher =
        new EventDispatcher.Impl.WithPublicDispatch<>();

    private final EventDispatcher.Impl.WithPublicDispatch<DeliverWaystoneAtLocationEvent.Packet> requestedWaystoneAtLocationEventDispatcher =
        new EventDispatcher.Impl.WithPublicDispatch<>();

    public Optional<String> update(String newName, WaystoneLocationData location) {
        if(!Signpost.getServerType().isServer || location.blockLocation.world.match(w -> !(w instanceof ServerWorld), i -> false)) {
            PacketHandler.sendToServer(new WaystoneUpdatedEventEvent.Packet(WaystoneUpdatedEvent.fromUpdated(location, newName)));
            return Optional.empty();
        } else {
            UUID[] oldIds = allWaystones
                .entrySet()
                .stream()
                .filter(e -> e.getValue().locationData.blockLocation.equals(location.blockLocation))
                .map(Map.Entry::getKey)
                .distinct()
                .toArray(UUID[]::new);
            String[] oldNames = Arrays.stream(oldIds).map(id -> allWaystones.get(id).name).toArray(String[]::new);
            if(oldIds.length > 1)
                Signpost.LOGGER.error("Waystone at " + location + ", new name: " + newName +" was already present "
                    + oldIds.length + " times. This indicates invalid state. Names found: " + String.join(", ", oldNames));
            for(UUID oldId: oldIds){
                allWaystones.remove(oldId);
            }
            UUID id = oldIds.length > 0 ? oldIds[0] : UUID.randomUUID();
            allWaystones.put(id, new WaystoneEntry(newName, location));
            Optional<String> oldName = oldNames.length > 0 ? Optional.of(oldNames[0]) : Optional.empty();
            _updateEventDispatcher.dispatch(WaystoneUpdatedEvent.fromUpdated(
                location,
                newName,
                oldName
            ), false);
            PacketHandler.sendToAll(new WaystoneUpdatedEventEvent.Packet(
                WaystoneUpdatedEvent.fromUpdated(location, newName, oldName)));
            markDirty();
            return oldName;
        }
    }

    public boolean remove(String name) {
        assert Signpost.getServerType().isServer;
        Optional<Map.Entry<UUID, WaystoneEntry>> oldEntry = getByName(name);
        return oldEntry.isPresent() && remove(oldEntry.get().getKey());
    }

    public boolean removeAt(WorldLocation location) {
        assert Signpost.getServerType().isServer;
        Optional<Map.Entry<UUID, WaystoneEntry>> oldEntry = getByLocation(location);
        return oldEntry.isPresent() && remove(oldEntry.get().getKey());
    }

    public boolean remove(UUID id) {
        assert Signpost.getServerType().isServer;
        WaystoneEntry oldEntry = allWaystones.remove(id);
        if(oldEntry == null) return false;
        else {
            _updateEventDispatcher.dispatch(new WaystoneRemovedEvent(oldEntry.locationData, oldEntry.name), false);
            PacketHandler.sendToAll(new WaystoneUpdatedEventEvent.Packet(new WaystoneRemovedEvent(oldEntry.locationData, oldEntry.name)));
            markDirty();
            return true;
        }
    }

    public boolean updateLocation(WorldLocation oldLocation, WorldLocation newLocation) {
        assert Signpost.getServerType().isServer;
        Optional<Map.Entry<UUID, WaystoneEntry>> oldEntry = getByLocation(oldLocation);
        if(!oldEntry.isPresent()) return false;
        else {
            allWaystones.remove(oldEntry.get().getKey());
            Vector3 newSpawnLocation = oldEntry.get().getValue().locationData.spawnPosition
                .add(Vector3.fromBlockPos(newLocation.blockPos.subtract(oldLocation.blockPos)));
            allWaystones.put(oldEntry.get().getKey(), new WaystoneEntry(oldEntry.get().getValue().name, new WaystoneLocationData(newLocation, newSpawnLocation)));
            _updateEventDispatcher.dispatch(new WaystoneMovedEvent(oldEntry.get().getValue().locationData, newLocation, oldEntry.get().getValue().name), false);
            markDirty();
            return true;
        }
    }

    private Optional<Map.Entry<UUID, WaystoneEntry>> getByName(String name){
        assert Signpost.getServerType().isServer;
        return allWaystones.entrySet().stream()
            .filter(e -> e.getValue().name.equals(name)).findFirst();
    }

    private Optional<Map.Entry<UUID, WaystoneEntry>> getByLocation(WorldLocation location){
        assert Signpost.getServerType().isServer;
        return allWaystones.entrySet().stream()
            .filter(e -> e.getValue().locationData.blockLocation.equals(location)).findFirst();
    }

    public void requestAllWaystoneNames(Consumer<Map<UUID, String>> onReply) {
        if(Signpost.getServerType().isServer){
            onReply.accept(getAllWaystoneNames());
        } else {
            requestedAllNamesEventDispatcher.addListener(onReply);
            PacketHandler.sendToServer(new RequestAllWaystoneNamesEvent.Packet());
        }
    }

    public void requestIdFor(String text, Consumer<Optional<UUID>> onReply) {
        if(Signpost.getServerType().isServer){
            onReply.accept(getIdFor(text));
        } else {
            requestedIdEventDispatcher.addListener(onReply);
            PacketHandler.sendToServer(new RequestIdEvent.Packet(text));
        }
    }

    private Optional<UUID> getIdFor(String name){
        return allWaystones.entrySet().stream()
            .filter(e -> e.getValue().name.equals(name))
            .map(Map.Entry::getKey)
            .findFirst();
    }

    private Map<UUID, String> getAllWaystoneNames(){
        assert Signpost.getServerType().isServer;
        return getInstance().allWaystones.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().name));
    }

    private Optional<WaystoneData> tryGetWaystoneDataAt(WorldLocation location) {
        return getInstance().allWaystones.values().stream()
            .filter(e -> e.locationData.blockLocation.equals(location))
            .findFirst()
            .map(entry -> new WaystoneData(entry.name, entry.locationData.spawnPosition));
    }

    public void requestWaystoneDataAtLocation(WorldLocation location, Consumer<Optional<WaystoneData>> onReply) {
        if(Signpost.getServerType().isServer) {
            onReply.accept(tryGetWaystoneDataAt(location));
        } else {
            requestedWaystoneAtLocationEventDispatcher.addListener(new Consumer<DeliverWaystoneAtLocationEvent.Packet>() {
                @Override
                public void accept(DeliverWaystoneAtLocationEvent.Packet packet) {
                    if (packet.waystoneLocation.equals(location)) {
                        requestedWaystoneAtLocationEventDispatcher.removeListener(this);
                        onReply.accept(packet.data);
                    }
                }
            });
            PacketHandler.sendToServer(new RequestWaystoneAtLocationEvent.Packet(location));
        }
    }

    private void markDirty(){ savedData.markDirty(); }

    private static final class RequestAllWaystoneNamesEvent implements PacketHandler.Event<RequestAllWaystoneNamesEvent.Packet> {

        public static final class Packet {}

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, PacketBuffer buffer) { }

        @Override
        public Packet decode(PacketBuffer buffer) { return new Packet(); }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> context) {
            PacketHandler.send(
                PacketDistributor.PLAYER.with(() -> context.get().getSender()),
                new DeliverAllWaystoneNamesEvent.Packet(getInstance().getAllWaystoneNames()));
        }

    }

    private static final class DeliverAllWaystoneNamesEvent implements PacketHandler.Event<DeliverAllWaystoneNamesEvent.Packet> {

        public static final class Packet {
            public final Map<UUID, String> names;

            private Packet(Map<UUID, String> names) {
                this.names = names;
            }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, PacketBuffer buffer) {
            buffer.writeInt(message.names.size());
            for (Map.Entry<UUID, String> name: message.names.entrySet()) {
                buffer.writeUniqueId(name.getKey());
                buffer.writeString(name.getValue());
            }
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            Map<UUID, String> names = new HashMap<>();
            int count = buffer.readInt();
            for(int i = 0; i < count; i++)
                names.put(buffer.readUniqueId(), buffer.readString());
            return new Packet(names);
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> getInstance().requestedAllNamesEventDispatcher.dispatch(message.names, true));
        }
    }

    private static final class WaystoneUpdatedEventEvent implements PacketHandler.Event<WaystoneUpdatedEventEvent.Packet> {

        public static final class Packet {
            public final WaystoneUpdatedEvent event;
            private Packet(WaystoneUpdatedEvent event) { this.event = event; }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, PacketBuffer buffer) {
            WaystoneUpdatedEvent.Serializer.INSTANCE.writeTo(message.event, buffer);
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            return new Packet(WaystoneUpdatedEvent.Serializer.INSTANCE.readFrom(buffer));
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> contextProvider) {
            final NetworkEvent.Context context = contextProvider.get();
            context.enqueueWork(() -> {
                if(context.getDirection().getReceptionSide().isServer()){
                    switch (message.event.getType()){
                        case Added:
                        case Renamed:
                            getInstance().update(message.event.name, message.event.location);
                            break;
                        case Removed:
                            getInstance().remove(message.event.name);
                            break;
                        case Moved:
                            getInstance().updateLocation(message.event.location.blockLocation, ((WaystoneMovedEvent)message.event).newLocation);
                        default: throw new RuntimeException("Type " + message.event.getType() + " is not supported");
                    }
                } else getInstance()._updateEventDispatcher.dispatch(message.event, false);
            });
        }

    }

    private static final class RequestWaystoneAtLocationEvent implements PacketHandler.Event<RequestWaystoneAtLocationEvent.Packet> {

        public static final class Packet {
            public final WorldLocation waystoneLocation;

            public Packet(WorldLocation waystoneLocation) { this.waystoneLocation = waystoneLocation; }
        }

        @Override
        public Class<Packet> getMessageClass() {
            return Packet.class;
        }

        @Override
        public void encode(Packet message, PacketBuffer buffer) {
            WorldLocation.SERIALIZER.writeTo(message.waystoneLocation, buffer);
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            return new Packet(WorldLocation.SERIALIZER.readFrom(buffer));
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> context) {
            Optional<WaystoneData> dataAt = getInstance().tryGetWaystoneDataAt(message.waystoneLocation);
            PacketHandler.send(
                PacketDistributor.PLAYER.with(() -> context.get().getSender()),
                new DeliverWaystoneAtLocationEvent.Packet(
                    message.waystoneLocation,
                    dataAt
                ));
        }

    }

    private static final class DeliverWaystoneAtLocationEvent implements PacketHandler.Event<DeliverWaystoneAtLocationEvent.Packet> {

        private static final class Packet {
            private final WorldLocation waystoneLocation;
            private final Optional<WaystoneData> data;

            public Packet(WorldLocation waystoneLocation, Optional<WaystoneData> data) {
                this.waystoneLocation = waystoneLocation;
                this.data = data;
            }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, PacketBuffer buffer) {
            WorldLocation.SERIALIZER.writeTo(message.waystoneLocation, buffer);
            new OptionalSerializer<>(WaystoneData.SERIALIZER).writeTo(message.data, buffer);
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            return new Packet(
                WorldLocation.SERIALIZER.readFrom(buffer),
                new OptionalSerializer<>(WaystoneData.SERIALIZER).readFrom(buffer)
            );
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> getInstance().requestedWaystoneAtLocationEventDispatcher.dispatch(message, false));
        }

    }

    private static final class RequestIdEvent implements PacketHandler.Event<RequestIdEvent.Packet> {

        public static final class Packet {
            public final String name;
            public Packet(String name) {
                this.name = name;
            }
        }

        @Override
        public Class<Packet> getMessageClass() {
            return Packet.class;
        }

        @Override
        public void encode(Packet message, PacketBuffer buffer) {
            buffer.writeString(message.name);
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            return new Packet(buffer.readString());
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> context) {
            PacketHandler.send(
                PacketDistributor.PLAYER.with(() -> context.get().getSender()),
                new DeliverIdEvent.Packet(getInstance().getIdFor(message.name)));
        }

    }

    private static final class DeliverIdEvent implements PacketHandler.Event<DeliverIdEvent.Packet> {

        private static final class Packet {
            private final Optional<UUID> id;
            private Packet(Optional<UUID> id) {
                this.id = id;
            }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, PacketBuffer buffer) {
            OptionalSerializer.UUID.writeTo(message.id, buffer);
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            return new Packet(OptionalSerializer.UUID.readFrom(buffer));
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> getInstance().requestedIdEventDispatcher.dispatch(message.id, true));
        }

    }

    public CompoundNBT saveTo(CompoundNBT compound) {
        ListNBT waystones = new ListNBT();
        waystones.addAll(
            allWaystones.entrySet().stream().map(entry -> {
                CompoundNBT entryCompound = new CompoundNBT();
                entryCompound.putUniqueId("Id", entry.getKey());
                entryCompound.putString("Name", entry.getValue().name);
                WaystoneLocationData.Serializer.INSTANCE.writeTo(entry.getValue().locationData, entryCompound, "Location");
                return entryCompound;
            }).collect(Collectors.toSet()));
        compound.put("Waystones", waystones);

        ListNBT memory = new ListNBT();
        memory.addAll(
            playerMemory.entrySet().stream().map(entry -> {
                CompoundNBT entryCompound = new CompoundNBT();
                entryCompound.putUniqueId("Player", entry.getKey());
                ListNBT known = new ListNBT();
                known.addAll(entry.getValue().stream().map(NBTUtil::writeUniqueId).collect(Collectors.toSet()));
                entryCompound.put("DiscoveredWaystones", known);
                return entryCompound;
            }).collect(Collectors.toSet())
        );
        compound.put("PlayerMemory", memory);
        return compound;
    }

    public void readFrom(CompoundNBT compound) {
        allWaystones.clear();
        INBT dynamicWaystones = compound.get("Waystones");
        if(dynamicWaystones instanceof ListNBT) {
            for(INBT dynamicEntry : ((ListNBT) dynamicWaystones)) {
                if(dynamicEntry instanceof CompoundNBT) {
                    CompoundNBT entry = (CompoundNBT) dynamicEntry;
                    UUID id = entry.getUniqueId("Id");
                    String name = entry.getString("Name");
                    WaystoneLocationData location = WaystoneLocationData.Serializer.INSTANCE.read(entry, "Location");
                    allWaystones.put(id, new WaystoneEntry(name, location));
                }
            }
        }

        playerMemory.clear();
        INBT dynamicPlayerMemory = compound.get("PlayerMemory");
        if(dynamicPlayerMemory instanceof ListNBT) {
            for(INBT dynamicEntry : ((ListNBT) dynamicPlayerMemory)) {
                if (dynamicEntry instanceof CompoundNBT) {
                    CompoundNBT entry = (CompoundNBT) dynamicEntry;
                    UUID player = entry.getUniqueId("Player");
                    INBT dynamicKnown = entry.get("DiscoveredWaystones");
                    Set<UUID> known = dynamicKnown instanceof ListNBT
                        ?  ((ListNBT) dynamicKnown).stream()
                            .filter(e -> e instanceof CompoundNBT)
                            .map(e -> NBTUtil.readUniqueId((CompoundNBT) e)).collect(Collectors.toSet())
                        : new HashSet<>();
                    playerMemory.put(player, known);
                }
            }
        }
    }

}