package gollorum.signpost;

import gollorum.signpost.minecraft.block.Waystone;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.events.*;
import gollorum.signpost.minecraft.storage.WaystoneLibraryStorage;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.*;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Util;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class WaystoneLibrary {

    private static WaystoneLibrary instance;
    public static WaystoneLibrary getInstance() { return instance; }
    public static boolean hasInstance() { return instance != null; }

    private WorldSavedData savedData;
    public boolean hasStorageBeenSetup() { return savedData != null; }

    public static void initialize() {
        instance = new WaystoneLibrary();
    }

    private final EventDispatcher.Impl.WithPublicDispatch<WaystoneUpdatedEvent> _updateEventDispatcher = new EventDispatcher.Impl.WithPublicDispatch<>();

    public final EventDispatcher<WaystoneUpdatedEvent> updateEventDispatcher = _updateEventDispatcher;

    public static void registerNetworkPackets() {
        PacketHandler.register(new RequestAllWaystoneNamesEvent());
        PacketHandler.register(new DeliverAllWaystoneNamesEvent());
        PacketHandler.register(new WaystoneUpdatedEventEvent());
        PacketHandler.register(new RequestWaystoneLocationEvent());
        PacketHandler.register(new DeliverWaystoneLocationEvent());
        PacketHandler.register(new RequestWaystoneAtLocationEvent());
        PacketHandler.register(new DeliverWaystoneAtLocationEvent());
        PacketHandler.register(new DeliverIdEvent());
        PacketHandler.register(new RequestIdEvent());
    }

    public void setupStorage(ServerWorld world){
        DimensionSavedDataManager storage = world.getSavedData();
        savedData = storage.getOrCreate(WaystoneLibraryStorage::new, WaystoneLibraryStorage.NAME);
    }

    private WaystoneLibrary() {
        updateEventDispatcher.addListener(event -> {
            if(isWaystoneNameCacheDirty) return;
            switch(event.getType()) {
                case Added:
                    cachedWaystoneNames.add(event.name);
                    break;
                case Removed:
                    cachedWaystoneNames.remove(event.name);
                    break;
                case Renamed:
                    cachedWaystoneNames.remove(((WaystoneRenamedEvent)event).oldName);
                    cachedWaystoneNames.add(event.name);
                    break;
            }
        });
    }

    public WaystoneLocationData getLocationData(WaystoneHandle waystoneId) {
        assert Signpost.getServerType().isServer;
        return allWaystones.get(waystoneId).locationData;
    }

    public WaystoneData getData(WaystoneHandle waystoneId) {
        assert Signpost.getServerType().isServer;
        WaystoneEntry entry = allWaystones.get(waystoneId);
        return new WaystoneData(waystoneId, entry.name, entry.locationData, entry.isLocked);
    }

    private static class WaystoneEntry {
        public final String name;
        public final WaystoneLocationData locationData;
        public final boolean isLocked;
        public WaystoneEntry(
            String name,
            WaystoneLocationData locationData,
            boolean isLocked
        ) {
            this.name = name;
            this.locationData = locationData;
            this.isLocked = isLocked;
        }

        public boolean hasThePermissionToEdit(PlayerEntity player) {
            return WaystoneData.hasThePermissionToEdit(player, locationData, isLocked);
        }

    }

    private final Map<WaystoneHandle, WaystoneEntry> allWaystones = new ConcurrentHashMap<>();
    private final Map<PlayerHandle, Set<WaystoneHandle>> playerMemory = new ConcurrentHashMap<>();

    private final Set<String> cachedWaystoneNames = new HashSet<>();
    private boolean isWaystoneNameCacheDirty = true;

    private final EventDispatcher.Impl.WithPublicDispatch<Map<WaystoneHandle, String>> requestedAllNamesEventDispatcher =
        new EventDispatcher.Impl.WithPublicDispatch<>();

    private final EventDispatcher.Impl.WithPublicDispatch<Optional<WaystoneHandle>> requestedIdEventDispatcher =
        new EventDispatcher.Impl.WithPublicDispatch<>();

    private final EventDispatcher.Impl.WithPublicDispatch<DeliverWaystoneAtLocationEvent.Packet> requestedWaystoneAtLocationEventDispatcher =
        new EventDispatcher.Impl.WithPublicDispatch<>();

    private final EventDispatcher.Impl.WithPublicDispatch<DeliverWaystoneLocationEvent.Packet> requestedWaystoneLocationEventDispatcher =
        new EventDispatcher.Impl.WithPublicDispatch<>();

    public Optional<String> update(String newName, WaystoneLocationData location, @Nullable PlayerEntity editingPlayer, boolean isLocked) {
        if(!Signpost.getServerType().isServer || location.block.world.match(w -> !(w instanceof ServerWorld), i -> false)) {
            PacketHandler.sendToServer(new WaystoneUpdatedEventEvent.Packet(WaystoneUpdatedEvent.fromUpdated(location, newName, isLocked)));
            return Optional.empty();
        } else {
            WaystoneHandle[] oldWaystones = allWaystones
                .entrySet()
                .stream()
                .filter(e -> e.getValue().locationData.block.equals(location.block))
                .map(Map.Entry::getKey)
                .distinct()
                .toArray(WaystoneHandle[]::new);
            String[] oldNames = Arrays.stream(oldWaystones).map(id -> allWaystones.get(id).name).toArray(String[]::new);
            if(oldWaystones.length > 1)
                Signpost.LOGGER.error("Waystone at " + location + ", new name: " + newName +" was already present "
                    + oldWaystones.length + " times. This indicates invalid state. Names found: " + String.join(", ", oldNames));
            if(oldWaystones.length > 0) {
                WaystoneEntry oldEntry = allWaystones.get(oldWaystones[0]);
                if(editingPlayer != null && !oldEntry.hasThePermissionToEdit(editingPlayer)) {
                    // This should not happen unless a player tries to hacc
                    editingPlayer.sendMessage(new TranslationTextComponent(LangKeys.noPermissionWaystone), Util.DUMMY_UUID);
                    return Optional.empty();
                }
                if(editingPlayer != null && !WaystoneData.hasSecurityPermissions(editingPlayer, location))
                    isLocked = oldEntry.isLocked;
            }
            for(WaystoneHandle oldId: oldWaystones) {
                allWaystones.remove(oldId);
            }
            WaystoneHandle id = oldWaystones.length > 0 ? oldWaystones[0] : new WaystoneHandle(UUID.randomUUID());
            allWaystones.put(id, new WaystoneEntry(newName, location, isLocked));
            Optional<String> oldName = oldNames.length > 0 ? Optional.of(oldNames[0]) : Optional.empty();
            WaystoneUpdatedEvent updatedEvent = WaystoneUpdatedEvent.fromUpdated(
                location,
                newName,
                oldName,
                isLocked
            );
            _updateEventDispatcher.dispatch(updatedEvent, false);
            PacketHandler.sendToAll(new WaystoneUpdatedEventEvent.Packet(updatedEvent));
            markDirty();
            Waystone.discover(PlayerHandle.from(editingPlayer), new WaystoneData(id, newName, location, isLocked));
            return oldName;
        }
    }

    public boolean remove(String name, PlayerHandle playerHandle) {
        assert Signpost.getServerType().isServer;
        Optional<Map.Entry<WaystoneHandle, WaystoneEntry>> oldEntry = getByName(name);
        return oldEntry.isPresent() && remove(oldEntry.get().getKey(), playerHandle);
    }

    public boolean removeAt(WorldLocation location, PlayerHandle playerHandle) {
        assert Signpost.getServerType().isServer;
        Optional<Map.Entry<WaystoneHandle, WaystoneEntry>> oldEntry = getByLocation(location);
        return oldEntry.isPresent() && remove(oldEntry.get().getKey(), playerHandle);
    }

    public boolean remove(WaystoneHandle id, PlayerHandle playerHandle) {
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

    public boolean updateLocation(
        WorldLocation oldLocation,
        WorldLocation newLocation
    ) {
        assert Signpost.getServerType().isServer;
        Optional<Map.Entry<WaystoneHandle, WaystoneEntry>> oldEntry = getByLocation(oldLocation);
        if(!oldEntry.isPresent()) return false;
        else {
            allWaystones.remove(oldEntry.get().getKey());
            Vector3 newSpawnLocation = oldEntry.get().getValue().locationData.spawn
                .add(Vector3.fromBlockPos(newLocation.blockPos.subtract(oldLocation.blockPos)));
            allWaystones.put(oldEntry.get().getKey(), new WaystoneEntry(oldEntry.get().getValue().name, new WaystoneLocationData(newLocation, newSpawnLocation),
                oldEntry.get().getValue().isLocked));
            _updateEventDispatcher.dispatch(new WaystoneMovedEvent(oldEntry.get().getValue().locationData, newLocation, oldEntry.get().getValue().name), false);
            markDirty();
            return true;
        }
    }

    public Optional<WaystoneHandle> getHandleByName(String name){
        assert Signpost.getServerType().isServer;
        return getByName(name).map(e -> e.getKey());
    }

    public Optional<WaystoneHandle> getHandleByLocation(WorldLocation location){
        assert Signpost.getServerType().isServer;
        return getByLocation(location).map(e -> e.getKey());
    }

    private Optional<Map.Entry<WaystoneHandle, WaystoneEntry>> getByName(String name){
        assert Signpost.getServerType().isServer;
        return allWaystones.entrySet().stream()
            .filter(e -> e.getValue().name.equals(name)).findFirst();
    }

    private Optional<Map.Entry<WaystoneHandle, WaystoneEntry>> getByLocation(WorldLocation location){
        assert Signpost.getServerType().isServer;
        return allWaystones.entrySet().stream()
            .filter(e -> e.getValue().locationData.block.equals(location)).findFirst();
    }

    public void requestAllWaystoneNames(Consumer<Map<WaystoneHandle, String>> onReply, Optional<PlayerHandle> onlyKnownBy) {
        if(Signpost.getServerType().isServer) {
            onReply.accept(getAllWaystoneNamesAndHandles(onlyKnownBy));
        } else {
            requestedAllNamesEventDispatcher.addListener(onReply);
            PacketHandler.sendToServer(new RequestAllWaystoneNamesEvent.Packet(onlyKnownBy));
        }
    }

    public void requestIdFor(String text, Consumer<Optional<WaystoneHandle>> onReply) {
        if(Signpost.getServerType().isServer){
            onReply.accept(getHandleFor(text));
        } else {
            requestedIdEventDispatcher.addListener(onReply);
            PacketHandler.sendToServer(new RequestIdEvent.Packet(text));
        }
    }

    private Optional<WaystoneHandle> getHandleFor(String name){
        return allWaystones.entrySet().stream()
            .filter(e -> e.getValue().name.equals(name))
            .map(Map.Entry::getKey)
            .findFirst();
    }

    private Map<WaystoneHandle, String> getAllWaystoneNamesAndHandles(Optional<PlayerHandle> onlyKnownBy) {
        assert Signpost.getServerType().isServer;
        Map<WaystoneHandle, String> ret = getInstance().allWaystones.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().name));
        if(isWaystoneNameCacheDirty) {
            cachedWaystoneNames.clear();
            cachedWaystoneNames.addAll(ret.values());
            isWaystoneNameCacheDirty = false;
        }
        if(onlyKnownBy.isPresent() && Config.Server.teleport.enforceDiscovery.get()) {
            Set<WaystoneHandle> known = playerMemory.computeIfAbsent(onlyKnownBy.get(), h -> new HashSet<>());
            return ret.entrySet().stream()
                .filter(e -> known.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return ret;
    }

    public Optional<Set<String>> getAllWaystoneNames() {
        if(isWaystoneNameCacheDirty) {
            requestAllWaystoneNames(c -> {}, Optional.empty());
        }
        return isWaystoneNameCacheDirty
            ? Optional.empty()
            : Optional.of(new HashSet<>(cachedWaystoneNames));
    }

    private Optional<WaystoneData> tryGetWaystoneDataAt(WorldLocation location) {
        return getInstance().allWaystones.entrySet().stream()
            .filter(e -> e.getValue().locationData.block.equals(location))
            .findFirst()
            .map(entry -> new WaystoneData(
                entry.getKey(),
                entry.getValue().name,
                entry.getValue().locationData,
                entry.getValue().isLocked
            ));
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

    public void requestWaystoneLocationData(String waystoneName, Consumer<Optional<WaystoneLocationData>> onReply) {
        if(Signpost.getServerType().isServer) {
            onReply.accept(getByName(waystoneName).map(e -> e.getValue().locationData));
        } else {
            requestedWaystoneLocationEventDispatcher.addListener(new Consumer<DeliverWaystoneLocationEvent.Packet>() {
                @Override
                public void accept(DeliverWaystoneLocationEvent.Packet packet) {
                    if (packet.name.equals(waystoneName)) {
                        requestedWaystoneLocationEventDispatcher.removeListener(this);
                        onReply.accept(packet.data);
                    }
                }
            });
            PacketHandler.sendToServer(new RequestWaystoneLocationEvent.Packet(waystoneName));
        }
    }

    public boolean addDiscovered(PlayerHandle player, WaystoneHandle waystone) {
        assert Signpost.getServerType().isServer;
        if(playerMemory.computeIfAbsent(player, p -> new HashSet<>()).add(waystone)) {
            markDirty();
            return true;
        } return false;
    }

    public boolean isDiscovered(PlayerHandle player, WaystoneHandle waystone) {
        if(!playerMemory.containsKey(player))
            playerMemory.put(player, new HashSet<>());
        return playerMemory.get(player).contains(waystone);
    }

    public boolean contains(WaystoneHandle waystone) {
        assert Signpost.getServerType().isServer;
        return allWaystones.containsKey(waystone);
    }

    public void markDirty(){ savedData.markDirty(); }

    private static final class RequestAllWaystoneNamesEvent implements PacketHandler.Event<RequestAllWaystoneNamesEvent.Packet> {

        public static final class Packet {
            public final Optional<PlayerHandle> onlyKnownBy;

            public Packet(Optional<PlayerHandle> onlyKnownBy) { this.onlyKnownBy = onlyKnownBy; }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, PacketBuffer buffer) {
            PlayerHandle.Serializer.optional().write(message.onlyKnownBy, buffer);
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            return new Packet(PlayerHandle.Serializer.optional().read(buffer));
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> context) {
            PacketHandler.send(
                PacketDistributor.PLAYER.with(() -> context.get().getSender()),
                new DeliverAllWaystoneNamesEvent.Packet(getInstance().getAllWaystoneNamesAndHandles(message.onlyKnownBy)));
        }

    }

    private static final class DeliverAllWaystoneNamesEvent implements PacketHandler.Event<DeliverAllWaystoneNamesEvent.Packet> {

        public static final class Packet {
            public final Map<WaystoneHandle, String> names;

            private Packet(Map<WaystoneHandle, String> names) {
                this.names = names;
            }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, PacketBuffer buffer) {
            buffer.writeInt(message.names.size());
            for (Map.Entry<WaystoneHandle, String> name: message.names.entrySet()) {
                buffer.writeUniqueId(name.getKey().id);
                buffer.writeString(name.getValue());
            }
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            Map<WaystoneHandle, String> names = new HashMap<>();
            int count = buffer.readInt();
            for(int i = 0; i < count; i++)
                names.put(new WaystoneHandle(buffer.readUniqueId()), buffer.readString(32767));
            return new Packet(names);
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> context) {
            getInstance().cachedWaystoneNames.clear();
            getInstance().cachedWaystoneNames.addAll(message.names.values());
            getInstance().isWaystoneNameCacheDirty = false;
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
            WaystoneUpdatedEvent.Serializer.INSTANCE.write(message.event, buffer);
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            return new Packet(WaystoneUpdatedEvent.Serializer.INSTANCE.read(buffer));
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> contextProvider) {
            final NetworkEvent.Context context = contextProvider.get();
            context.enqueueWork(() -> {
                if(context.getDirection().getReceptionSide().isServer()){
                    PlayerEntity player = context.getSender();
                    switch (message.event.getType()){
                        case Added:
                            if(!TileEntityUtils.findTileEntityAt(message.event.location.block, WaystoneContainer.class).isPresent()) {
                                Signpost.LOGGER.error("Tried to add a waystone where no compatible TileEntity was present: " + message.event.location.block);
                                return;
                            }
                        case Renamed:
                            getInstance().update(message.event.name, message.event.location, player, ((WaystoneAddedOrRenamedEvent)message.event).isLocked);
                            break;
                        case Removed:
                            getInstance().remove(message.event.name, PlayerHandle.from(player));
                            break;
                        case Moved:
                            getInstance().updateLocation(
                                message.event.location.block,
                                ((WaystoneMovedEvent)message.event).newLocation
                            );
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
            WorldLocation.SERIALIZER.write(message.waystoneLocation, buffer);
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            return new Packet(WorldLocation.SERIALIZER.read(buffer));
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
            WorldLocation.SERIALIZER.write(message.waystoneLocation, buffer);
            WaystoneData.SERIALIZER.optional().write(message.data, buffer);
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            return new Packet(
                WorldLocation.SERIALIZER.read(buffer),
                WaystoneData.SERIALIZER.optional().read(buffer)
            );
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> getInstance().requestedWaystoneAtLocationEventDispatcher.dispatch(message, false));
        }

    }

    private static final class RequestWaystoneLocationEvent implements PacketHandler.Event<RequestWaystoneLocationEvent.Packet> {

        public static final class Packet {
            public final String name;

            public Packet(String name) { this.name = name; }
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
            return new Packet(buffer.readString(32767));
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> context) {
            Optional<WaystoneLocationData> dataAt = getInstance().getByName(message.name).map(e -> e.getValue().locationData);
            PacketHandler.send(
                PacketDistributor.PLAYER.with(() -> context.get().getSender()),
                new DeliverWaystoneLocationEvent.Packet(
                    message.name,
                    dataAt
                ));
        }

    }

    private static final class DeliverWaystoneLocationEvent implements PacketHandler.Event<DeliverWaystoneLocationEvent.Packet> {

        private static final class Packet {
            private final String name;
            private final Optional<WaystoneLocationData> data;

            public Packet(String name, Optional<WaystoneLocationData> data) {
                this.name = name;
                this.data = data;
            }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, PacketBuffer buffer) {
            buffer.writeString(message.name);
            WaystoneLocationData.SERIALIZER.optional()
                .write(message.data, buffer);
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            return new Packet(
                buffer.readString(32767),
                WaystoneLocationData.SERIALIZER.optional().
	                read(buffer)
            );
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> getInstance().requestedWaystoneLocationEventDispatcher.dispatch(message, false));
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
            return new Packet(buffer.readString(32767));
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> context) {
            PacketHandler.send(
                PacketDistributor.PLAYER.with(() -> context.get().getSender()),
                new DeliverIdEvent.Packet(getInstance().getHandleFor(message.name)));
        }

    }

    private static final class DeliverIdEvent implements PacketHandler.Event<DeliverIdEvent.Packet> {

        private static final class Packet {
            private final Optional<WaystoneHandle> waystone;
            private Packet(Optional<WaystoneHandle> waystone) {
                this.waystone = waystone;
            }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, PacketBuffer buffer) {
           WaystoneHandle.Serializer.optional().write(message.waystone, buffer);
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            return new Packet(WaystoneHandle.Serializer.optional().read(buffer));
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> getInstance().requestedIdEventDispatcher.dispatch(message.waystone, true));
        }

    }

    public CompoundNBT saveTo(CompoundNBT compound) {
        ListNBT waystones = new ListNBT();
        waystones.addAll(
            allWaystones.entrySet().stream().map(entry -> {
                CompoundNBT entryCompound = new CompoundNBT();
                entryCompound.put("Waystone", WaystoneHandle.Serializer.write(entry.getKey()));
                entryCompound.putString("Name", entry.getValue().name);
                entryCompound.put("Location", WaystoneLocationData.SERIALIZER.write(entry.getValue().locationData));
                entryCompound.putBoolean("IsLocked", entry.getValue().isLocked);
                return entryCompound;
            }).collect(Collectors.toSet()));
        compound.put("Waystones", waystones);

        ListNBT memory = new ListNBT();
        memory.addAll(
            playerMemory.entrySet().stream().map(entry -> {
                CompoundNBT entryCompound = new CompoundNBT();
                entryCompound.putUniqueId("Player", entry.getKey().id);
                ListNBT known = new ListNBT();
                known.addAll(entry.getValue().stream().map(WaystoneHandle.Serializer::write).collect(Collectors.toSet()));
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
                    WaystoneHandle waystone = WaystoneHandle.Serializer.read(entry.getCompound("Waystone"));
                    String name = entry.getString("Name");
                    WaystoneLocationData location = WaystoneLocationData.SERIALIZER.read(entry.getCompound("Location"));
                    boolean isLocked = entry.getBoolean("IsLocked");
                    allWaystones.put(waystone, new WaystoneEntry(name, location, isLocked));
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
                    Set<WaystoneHandle> known = dynamicKnown instanceof ListNBT
                        ?  ((ListNBT) dynamicKnown).stream()
                            .filter(e -> e instanceof CompoundNBT)
                            .map(e -> WaystoneHandle.Serializer.read((CompoundNBT) e))
                            .collect(Collectors.toSet())
                        : new HashSet<>();
                    playerMemory.put(new PlayerHandle(player), known);
                }
            }
        }
    }

}