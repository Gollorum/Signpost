package gollorum.signpost;

import gollorum.signpost.blockpartdata.types.renderers.BlockPartWaystoneUpdateListener;
import gollorum.signpost.compat.ExternalWaystoneLibrary;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.events.*;
import gollorum.signpost.minecraft.storage.WaystoneLibraryStorage;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.*;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.StringSerializer;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.openjdk.nashorn.internal.runtime.options.Option;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class WaystoneLibrary {

    private static WaystoneLibrary instance;
    public static WaystoneLibrary getInstance() {
        if(instance == null) {
            initialize();
            Signpost.LOGGER.warn("Force-initialized waystone library. This should not happen.");
        }
        return instance;
    }
    public static boolean hasInstance() { return instance != null; }

    private static final EventDispatcher.Impl.WithPublicDispatch<WaystoneLibrary> _initializeEventDispatcher =
        new EventDispatcher.Impl.WithPublicDispatch<>() {
            @Override
            public boolean addListener(@Nonnull Consumer<WaystoneLibrary> listener) {
                if(hasInstance()) listener.accept(instance);
                return super.addListener(listener);
            }
        };
    public static EventDispatcher<WaystoneLibrary> onInitializeDo = _initializeEventDispatcher;

    // Server only
    private SavedData savedData;
    public boolean hasStorageBeenSetup() { return savedData != null; }

    public static void initialize() {
        instance = new WaystoneLibrary();
        _initializeEventDispatcher.dispatch(instance, false);
    }

    private final EventDispatcher.Impl.WithPublicDispatch<WaystoneUpdatedEvent> _updateEventDispatcher =
        new EventDispatcher.Impl.WithPublicDispatch<>();

    public final EventDispatcher<WaystoneUpdatedEvent> updateEventDispatcher = _updateEventDispatcher;

    public static void registerNetworkPackets() {
        int id = -1000;
        PacketHandler.register(new RequestAllWaystoneNamesEvent(), id++);
        PacketHandler.register(new DeliverAllWaystoneNamesEvent(), id++);
        PacketHandler.register(new RequestAllWaystonesEvent(), id++);
        PacketHandler.register(new DeliverAllWaystonesEvent(), id++);
        PacketHandler.register(new WaystoneUpdatedEventEvent(), id++);
        PacketHandler.register(new RequestWaystoneLocationEvent(), id++);
        PacketHandler.register(new DeliverWaystoneLocationEvent(), id++);
        PacketHandler.register(new RequestWaystoneAtLocationEvent(), id++);
        PacketHandler.register(new DeliverWaystoneAtLocationEvent(), id++);
        PacketHandler.register(new DeliverIdEvent(), id++);
        PacketHandler.register(new RequestIdEvent(), id++);
    }

    public void setupStorage(ServerLevel world){
        DimensionDataStorage storage = world.getDataStorage();
        savedData = storage.computeIfAbsent(WaystoneLibraryStorage::load, WaystoneLibraryStorage::new, WaystoneLibraryStorage.NAME);
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

    public WaystoneLocationData getLocationData(WaystoneHandle.Vanilla waystoneId) {
        assert Signpost.getServerType().isServer;
        return allWaystones.get(waystoneId).locationData;
    }

    public Optional<WaystoneDataBase> getData(WaystoneHandle handle) {
        return handle instanceof WaystoneHandle.Vanilla
            ? getData((WaystoneHandle.Vanilla) handle).map(d -> d)
            : ExternalWaystoneLibrary.getInstance().getData(handle).map(d -> d);
    }

    public Optional<WaystoneData> getData(WaystoneHandle.Vanilla waystoneId) {
        assert Signpost.getServerType().isServer;
        WaystoneEntry entry = allWaystones.get(waystoneId);
        return entry == null
            ? Optional.empty()
            : Optional.of(new WaystoneData(waystoneId, entry.name, entry.locationData, entry.isLocked));
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

        public boolean hasThePermissionToEdit(Player player) {
            return WaystoneData.hasThePermissionToEdit(player, locationData, isLocked);
        }

    }

    public static final class WaystoneInfo {
        public final String name;
        public final WaystoneLocationData locationData;
        public final WaystoneHandle.Vanilla handle;

        public WaystoneInfo(String name, WaystoneLocationData locationData, WaystoneHandle.Vanilla handle) {
            this.name = name;
            this.locationData = locationData;
            this.handle = handle;
        }
    }

    private final Map<WaystoneHandle.Vanilla, WaystoneEntry> allWaystones = new ConcurrentHashMap<>();
    private final Map<PlayerHandle, Set<WaystoneHandle.Vanilla>> playerMemory = new ConcurrentHashMap<>();

    private final Set<String> cachedWaystoneNames = new HashSet<>();
    private boolean isWaystoneNameCacheDirty = true;

    private final EventDispatcher.Impl.WithPublicDispatch<Map<WaystoneHandle.Vanilla, String>> requestedAllNamesEventDispatcher =
        new EventDispatcher.Impl.WithPublicDispatch<>();

    private final EventDispatcher.Impl.WithPublicDispatch<Map<WaystoneHandle.Vanilla, Tuple<String, WaystoneLocationData>>> requestedAllWaystonesEventDispatcher =
        new EventDispatcher.Impl.WithPublicDispatch<>();

    private final EventDispatcher.Impl.WithPublicDispatch<Optional<WaystoneHandle.Vanilla>> requestedIdEventDispatcher =
        new EventDispatcher.Impl.WithPublicDispatch<>();

    private final EventDispatcher.Impl.WithPublicDispatch<DeliverWaystoneAtLocationEvent.Packet> requestedWaystoneAtLocationEventDispatcher =
        new EventDispatcher.Impl.WithPublicDispatch<>();

    private final EventDispatcher.Impl.WithPublicDispatch<DeliverWaystoneLocationEvent.Packet> requestedWaystoneLocationEventDispatcher =
        new EventDispatcher.Impl.WithPublicDispatch<>();

    public void requestUpdate(String newName, WaystoneLocationData location, boolean isLocked) {
        PacketHandler.sendToServer(new WaystoneUpdatedEventEvent.Packet(WaystoneUpdatedEvent.fromUpdated(location, newName, isLocked, WaystoneHandle.Vanilla.NIL)));
    }

    public Optional<String> update(String newName, WaystoneLocationData location, @Nullable Player editingPlayer, boolean isLocked) {
        assert Signpost.getServerType().isServer && location.block.world.match(w -> (w instanceof ServerLevel), i -> true);
        WaystoneHandle.Vanilla[] oldWaystones = allWaystones
            .entrySet()
            .stream()
            .filter(e -> e.getValue().locationData.block.equals(location.block))
            .map(Map.Entry::getKey)
            .distinct()
            .toArray(WaystoneHandle.Vanilla[]::new);
        String[] oldNames = Arrays.stream(oldWaystones).map(id -> allWaystones.get(id).name).toArray(String[]::new);
        if(oldWaystones.length > 1)
            Signpost.LOGGER.error("Waystone at " + location + " (new name: " + newName +") was already present "
                + oldWaystones.length + " times. This indicates invalid state. Names found: " + String.join(", ", oldNames));
        if(oldWaystones.length > 0) {
            WaystoneEntry oldEntry = allWaystones.get(oldWaystones[0]);
            if(editingPlayer != null && !oldEntry.hasThePermissionToEdit(editingPlayer)) {
                // This should not happen unless a player tries to hacc
                editingPlayer.sendMessage(new TranslatableComponent(LangKeys.noPermissionWaystone), Util.NIL_UUID);
                return Optional.empty();
            }
            if(editingPlayer != null && !gollorum.signpost.utils.WaystoneData.hasSecurityPermissions(editingPlayer, location))
                isLocked = oldEntry.isLocked;
            for(WaystoneHandle.Vanilla oldId: oldWaystones) {
                allWaystones.remove(oldId);
            }
        }
        if(!validateNameDoesNotExist(newName, editingPlayer)) return Optional.empty();
        WaystoneHandle.Vanilla id = oldWaystones.length > 0 ? oldWaystones[0] : new WaystoneHandle.Vanilla(UUID.randomUUID());
        allWaystones.put(id, new WaystoneEntry(newName, location, isLocked));
        Optional<String> oldName = oldNames.length > 0 ? Optional.of(oldNames[0]) : Optional.empty();
        WaystoneUpdatedEvent updatedEvent = WaystoneUpdatedEvent.fromUpdated(
            location,
            newName,
            oldName,
            isLocked,
            id
        );
        _updateEventDispatcher.dispatch(updatedEvent, false);
        PacketHandler.sendToAll(new WaystoneUpdatedEventEvent.Packet(updatedEvent));
        markDirty();
        WaystoneBlock.discover(PlayerHandle.from(editingPlayer), new WaystoneData(id, newName, location, isLocked));
        return oldName;
    }

    public boolean tryAddNew(String newName, WaystoneLocationData location, ServerPlayer editingPlayer, Optional<WaystoneHandle.Vanilla> handle) {
        if(handle.map(h -> !validateHandleDoesNotExist(h, editingPlayer)).orElse(false)) return false;
        if(!validateNameDoesNotExist(newName, editingPlayer)) return false;
        if(allWaystones.values().stream().anyMatch(entry -> entry.locationData.block.equals(location.block))) {
            Signpost.LOGGER.error("Waystone at " + location + " (new name: " + newName +") was already present. " +
                "This indicates invalid state.");
            return false;
        }
        WaystoneHandle.Vanilla id = handle.orElseGet(() -> new WaystoneHandle.Vanilla(UUID.randomUUID()));
        boolean isLocked = false;
        allWaystones.put(id, new WaystoneEntry(newName, location, isLocked));
        WaystoneUpdatedEvent updatedEvent = WaystoneUpdatedEvent.fromUpdated(
            location,
            newName,
            Optional.empty(),
            isLocked,
            id
        );
        _updateEventDispatcher.dispatch(updatedEvent, false);
        PacketHandler.sendToAll(new WaystoneUpdatedEventEvent.Packet(updatedEvent));
        markDirty();
        WaystoneBlock.discover(PlayerHandle.from(editingPlayer), new WaystoneData(id, newName, location, isLocked));
        return true;
    }

    private boolean validateHandleDoesNotExist(WaystoneHandle.Vanilla handle, Player editingPlayer) {
        if(allWaystones.containsKey(handle)) {
            editingPlayer.displayClientMessage(new TranslatableComponent(LangKeys.duplicateWaystoneId), true);
            return false;
        } else return true;
    }

    private boolean validateNameDoesNotExist(String newName, @Nullable Player editingPlayer) {
        if(allWaystones.values().stream().anyMatch(entry -> entry.name.equals(newName))) {
            if(editingPlayer != null)
                editingPlayer.displayClientMessage(new TranslatableComponent(LangKeys.duplicateWaystoneName, newName), true);
            else Signpost.LOGGER.error("Tried to automatically name a waystone \"" + newName + "\", which already existed.");
            return false;
        } else return true;
    }

    public boolean remove(String name, PlayerHandle playerHandle) {
        assert Signpost.getServerType().isServer;
        Optional<Map.Entry<WaystoneHandle.Vanilla, WaystoneEntry>> oldEntry = getByName(name);
        return oldEntry.isPresent() && remove(oldEntry.get().getKey(), playerHandle);
    }

    public boolean removeAt(WorldLocation location, PlayerHandle playerHandle) {
        assert Signpost.getServerType().isServer;
        Optional<Map.Entry<WaystoneHandle.Vanilla, WaystoneEntry>> oldEntry = getByLocation(location);
        return oldEntry.isPresent() && remove(oldEntry.get().getKey(), playerHandle);
    }

    public boolean remove(WaystoneHandle.Vanilla handle, PlayerHandle playerHandle) {
        assert Signpost.getServerType().isServer;
        WaystoneEntry oldEntry = allWaystones.remove(handle);
        if(oldEntry == null) return false;
        else {
            _updateEventDispatcher.dispatch(new WaystoneRemovedEvent(oldEntry.locationData, oldEntry.name,handle), false);
            PacketHandler.sendToAll(new WaystoneUpdatedEventEvent.Packet(new WaystoneRemovedEvent(oldEntry.locationData, oldEntry.name, handle)));
            markDirty();
            return true;
        }
    }

    public boolean updateLocation(
        WorldLocation oldLocation,
        WorldLocation newLocation
    ) {
        assert Signpost.getServerType().isServer;
        Optional<Map.Entry<WaystoneHandle.Vanilla, WaystoneEntry>> oldEntry = getByLocation(oldLocation);
        if(!oldEntry.isPresent()) return false;
        else {
            allWaystones.remove(oldEntry.get().getKey());
            Vector3 newSpawnLocation = oldEntry.get().getValue().locationData.spawn
                .add(Vector3.fromBlockPos(newLocation.blockPos.subtract(oldLocation.blockPos)));
            allWaystones.put(oldEntry.get().getKey(), new WaystoneEntry(oldEntry.get().getValue().name, new WaystoneLocationData(newLocation, newSpawnLocation),
                oldEntry.get().getValue().isLocked));
            _updateEventDispatcher.dispatch(new WaystoneMovedEvent(
                oldEntry.get().getValue().locationData,
                newLocation,
                oldEntry.get().getValue().name,
                oldEntry.get().getKey()
            ), false);
            markDirty();
            return true;
        }
    }

    public Optional<WaystoneHandle.Vanilla> getHandleByName(String name){
        assert Signpost.getServerType().isServer;
        return getByName(name).map(e -> e.getKey());
    }

    public Optional<WaystoneHandle.Vanilla> getHandleByLocation(WorldLocation location){
        assert Signpost.getServerType().isServer;
        return getByLocation(location).map(e -> e.getKey());
    }

    private Optional<Map.Entry<WaystoneHandle.Vanilla, WaystoneEntry>> getByName(String name){
        assert Signpost.getServerType().isServer;
        return allWaystones.entrySet().stream()
            .filter(e -> e.getValue().name.equals(name)).findFirst();
    }

    private Optional<Map.Entry<WaystoneHandle.Vanilla, WaystoneEntry>> getByLocation(WorldLocation location){
        assert Signpost.getServerType().isServer;
        return allWaystones.entrySet().stream()
            .filter(e -> e.getValue().locationData.block.equals(location)).findFirst();
    }

    public void requestAllWaystoneNames(Consumer<Map<WaystoneHandle.Vanilla, String>> onReply, Optional<PlayerHandle> onlyKnownBy, boolean isClient) {
        if (isClient) {
            requestedAllNamesEventDispatcher.addListener(onReply);
            PacketHandler.sendToServer(new RequestAllWaystoneNamesEvent.Packet(onlyKnownBy));
        } else {
            onReply.accept(getAllWaystoneNamesAndHandles(onlyKnownBy));
        }
    }

    public void requestAllWaystones(
        Consumer<Map<WaystoneHandle.Vanilla, Tuple<String, WaystoneLocationData>>> onReply,
        Optional<PlayerHandle> onlyKnownBy,
        boolean isClient
    ) {
        if (isClient) {
            requestedAllWaystonesEventDispatcher.addListener(onReply);
            PacketHandler.sendToServer(new RequestAllWaystonesEvent.Packet(onlyKnownBy));
        } else {
            onReply.accept(getAllWaystones(onlyKnownBy));
        }
    }

    public void requestWaystoneAt(WorldLocation location, Consumer<Optional<WaystoneData>> onReply, boolean isClient) {
        if(isClient) {
            requestedWaystoneAtLocationEventDispatcher.addListener(packet -> {
                if(packet.waystoneLocation.equals(location)) {
                    onReply.accept(packet.data);
                    return true;
                } else return false;
            });
            PacketHandler.sendToServer(new RequestWaystoneAtLocationEvent.Packet(location));
        } else onReply.accept(tryGetWaystoneDataAt(location));
    }

    private Optional<WaystoneHandle.Vanilla> getHandleFor(String name){
        return allWaystones.entrySet().stream()
            .filter(e -> e.getValue().name.equals(name))
            .map(Map.Entry::getKey)
            .findFirst();
    }

    private Map<WaystoneHandle.Vanilla, String> getAllWaystoneNamesAndHandles(Optional<PlayerHandle> onlyKnownBy) {
        assert Signpost.getServerType().isServer;
        Map<WaystoneHandle.Vanilla, String> ret = getInstance().allWaystones.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().name));
        if(isWaystoneNameCacheDirty) {
            cachedWaystoneNames.clear();
            cachedWaystoneNames.addAll(ret.values());
            isWaystoneNameCacheDirty = false;
        }
        if(onlyKnownBy.isPresent() && Config.Server.teleport.enforceDiscovery.get()) {
            Set<WaystoneHandle.Vanilla> known = playerMemory.computeIfAbsent(onlyKnownBy.get(), h -> new HashSet<>());
            return ret.entrySet().stream()
                .filter(e -> known.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return ret;
    }

    private Map<WaystoneHandle.Vanilla, Tuple<String, WaystoneLocationData>> getAllWaystones(Optional<PlayerHandle> onlyKnownBy) {
        assert Signpost.getServerType().isServer;
        Map<WaystoneHandle.Vanilla, Tuple<String, WaystoneLocationData>> ret = getInstance().allWaystones.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> Tuple.of(e.getValue().name, e.getValue().locationData)));
        if(onlyKnownBy.isPresent() && Config.Server.teleport.enforceDiscovery.get()) {
            PlayerHandle player = onlyKnownBy.get();
            Set<WaystoneHandle.Vanilla> known = playerMemory.computeIfAbsent(player, h -> new HashSet<>());
            return ret.entrySet().stream()
                .filter(e -> known.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return ret;
    }

    public Optional<Set<String>> getAllWaystoneNames(boolean isClient) {
        if(isWaystoneNameCacheDirty) {
            requestAllWaystoneNames(c -> {}, Optional.empty(), isClient);
        }
        return isWaystoneNameCacheDirty
            ? Optional.empty()
            : Optional.of(new HashSet<>(cachedWaystoneNames));
    }

    // Only on server
    public Set<WaystoneInfo> getAllWaystoneInfo() {
        assert Signpost.getServerType().isServer;
        return allWaystones.entrySet().stream().map(entry -> new WaystoneInfo(
            entry.getValue().name,
            entry.getValue().locationData,
            entry.getKey()
        )).collect(Collectors.toSet());
    }

    private Optional<WaystoneData> tryGetWaystoneDataAt(WorldLocation location) {
        assert Signpost.getServerType().isServer;
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

    public boolean addDiscovered(PlayerHandle player, WaystoneHandle.Vanilla waystone) {
        assert Signpost.getServerType().isServer;
        if(playerMemory.computeIfAbsent(player, p -> new HashSet<>()).add(waystone)) {
            markDirty();
            return true;
        } return false;
    }

    public boolean isDiscovered(PlayerHandle player, WaystoneHandle.Vanilla waystone) {
        if(!playerMemory.containsKey(player))
            playerMemory.put(player, new HashSet<>());
        return playerMemory.get(player).contains(waystone);
    }

    public boolean contains(WaystoneHandle.Vanilla waystone) {
        assert Signpost.getServerType().isServer;
        if(!allWaystones.containsKey(waystone)) return false;
        WaystoneEntry entry = allWaystones.get(waystone);
        return assertTileEntityExists(entry);
    }

    private boolean assertTileEntityExists(WaystoneEntry entry) {
        Optional<ServerLevel> level = TileEntityUtils.toWorld(entry.locationData.block.world, false)
            .flatMap(lv -> lv instanceof ServerLevel ? Optional.of((ServerLevel)lv) : Optional.empty());
        if(!level.isPresent()) return true; // Something is wrong, I cannot find the level to check.
        Supplier<Boolean> checkEntity = () -> {
            Optional<WaystoneTile> entity = TileEntityUtils.findTileEntity(level.get(), entry.locationData.block.blockPos, WaystoneTile.getBlockEntityType());
            if(entity.isPresent()) return true;
            else {
                WaystoneTile.onRemoved(level.get(), entry.locationData.block.blockPos);
                return false;
            }
        };
        if(level.get().thread != Thread.currentThread()) { // Cannot check on wrong thread.
            Delay.onServerForFrames(1, checkEntity::get);
            return true;
        } else return checkEntity.get();
    }

    public void markDirty(){
        // savedData is null on dedicated clients
        if(savedData != null) savedData.setDirty();
    }

    private static final class RequestAllWaystoneNamesEvent implements PacketHandler.Event<RequestAllWaystoneNamesEvent.Packet> {

        public static final class Packet {
            public final Optional<PlayerHandle> onlyKnownBy;

            public Packet(Optional<PlayerHandle> onlyKnownBy) { this.onlyKnownBy = onlyKnownBy; }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, FriendlyByteBuf buffer) {
            PlayerHandle.Serializer.optional().write(message.onlyKnownBy, buffer);
        }

        @Override
        public Packet decode(FriendlyByteBuf buffer) {
            return new Packet(PlayerHandle.Serializer.optional().read(buffer));
        }

        @Override
        public void handle(Packet message, NetworkEvent.Context context) {
            PacketHandler.send(
                PacketDistributor.PLAYER.with(context::getSender),
                new DeliverAllWaystoneNamesEvent.Packet(getInstance().getAllWaystoneNamesAndHandles(message.onlyKnownBy))
            );
        }

    }

    private static final class DeliverAllWaystoneNamesEvent implements PacketHandler.Event<DeliverAllWaystoneNamesEvent.Packet> {

        public static final class Packet {
            public final Map<WaystoneHandle.Vanilla, String> names;

            private Packet(Map<WaystoneHandle.Vanilla, String> names) {
                this.names = names;
            }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, FriendlyByteBuf buffer) {
            buffer.writeInt(message.names.size());
            for (Map.Entry<WaystoneHandle.Vanilla, String> name: message.names.entrySet()) {
                buffer.writeUUID(name.getKey().id);
                StringSerializer.instance.write(name.getValue(), buffer);
            }
        }

        @Override
        public Packet decode(FriendlyByteBuf buffer) {
            Map<WaystoneHandle.Vanilla, String> names = new HashMap<>();
            int count = buffer.readInt();
            for(int i = 0; i < count; i++)
                names.put(new WaystoneHandle.Vanilla(buffer.readUUID()), StringSerializer.instance.read(buffer));
            return new Packet(names);
        }

        @Override
        public void handle(Packet message, NetworkEvent.Context context) {
            getInstance().cachedWaystoneNames.clear();
            getInstance().cachedWaystoneNames.addAll(message.names.values());
            getInstance().isWaystoneNameCacheDirty = false;
            getInstance().requestedAllNamesEventDispatcher.dispatch(message.names, true);
        }
    }

    private static final class RequestAllWaystonesEvent implements PacketHandler.Event<RequestAllWaystonesEvent.Packet> {

        private static final CompoundSerializable<Optional<PlayerHandle>> serializer = PlayerHandle.Serializer.optional();

        public static final class Packet {
            public final Optional<PlayerHandle> onlyKnownBy;

            public Packet(Optional<PlayerHandle> onlyKnownBy) {
                this.onlyKnownBy = onlyKnownBy;
            }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, FriendlyByteBuf buffer) {
            serializer.write(message.onlyKnownBy, buffer);
        }

        @Override
        public Packet decode(FriendlyByteBuf buffer) {
            return new Packet(serializer.read(buffer));
        }

        @Override
        public void handle(Packet message, NetworkEvent.Context context) {
            PacketHandler.send(
                PacketDistributor.PLAYER.with(context::getSender),
                new DeliverAllWaystonesEvent.Packet(getInstance().getAllWaystones(message.onlyKnownBy))
            );
        }

    }

    private static final class DeliverAllWaystonesEvent implements PacketHandler.Event<DeliverAllWaystonesEvent.Packet> {

        public static final class Packet {
            public final Map<WaystoneHandle.Vanilla, Tuple<String, WaystoneLocationData>> data;

            private Packet(Map<WaystoneHandle.Vanilla, Tuple<String, WaystoneLocationData>> data) {
                this.data = data;
            }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, FriendlyByteBuf buffer) {
            buffer.writeInt(message.data.size());
            for (Map.Entry<WaystoneHandle.Vanilla, Tuple<String, WaystoneLocationData>> name: message.data.entrySet()) {
                buffer.writeUUID(name.getKey().id);
                buffer.writeUtf(name.getValue()._1);
                WaystoneLocationData.SERIALIZER.write(name.getValue()._2, buffer);
            }
        }

        @Override
        public Packet decode(FriendlyByteBuf buffer) {
            Map<WaystoneHandle.Vanilla, Tuple<String, WaystoneLocationData>> names = new HashMap<>();
            int count = buffer.readInt();
            for(int i = 0; i < count; i++)
                names.put(
                    new WaystoneHandle.Vanilla(buffer.readUUID()),
                    Tuple.of(
                        StringSerializer.instance.read(buffer),
                        WaystoneLocationData.SERIALIZER.read(buffer)
                    )
                );
            return new Packet(names);
        }

        @Override
        public void handle(Packet message, NetworkEvent.Context context) {
            getInstance().requestedAllWaystonesEventDispatcher.dispatch(message.data, true);
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
        public void encode(Packet message, FriendlyByteBuf buffer) {
            WaystoneUpdatedEvent.Serializer.INSTANCE.write(message.event, buffer);
        }

        @Override
        public Packet decode(FriendlyByteBuf buffer) {
            return new Packet(WaystoneUpdatedEvent.Serializer.INSTANCE.read(buffer));
        }

        @Override
        public void handle(Packet message, NetworkEvent.Context context) {
            if(context.getDirection().getReceptionSide().isServer()){
                Player player = context.getSender();
                switch (message.event.getType()){
                    case Added:
                        if(!TileEntityUtils.findTileEntityAt(message.event.location.block, WaystoneContainer.class, false).isPresent()) {
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
        public void encode(Packet message, FriendlyByteBuf buffer) {
            WorldLocation.SERIALIZER.write(message.waystoneLocation, buffer);
        }

        @Override
        public Packet decode(FriendlyByteBuf buffer) {
            return new Packet(WorldLocation.SERIALIZER.read(buffer));
        }

        @Override
        public void handle(Packet message, NetworkEvent.Context context) {
            Optional<WaystoneData> dataAt = getInstance().tryGetWaystoneDataAt(message.waystoneLocation);
            PacketHandler.send(
                PacketDistributor.PLAYER.with(context::getSender),
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
        public void encode(Packet message, FriendlyByteBuf buffer) {
            WorldLocation.SERIALIZER.write(message.waystoneLocation, buffer);
            WaystoneData.SERIALIZER.optional().write(message.data, buffer);
        }

        @Override
        public Packet decode(FriendlyByteBuf buffer) {
            return new Packet(
                WorldLocation.SERIALIZER.read(buffer),
                WaystoneData.SERIALIZER.optional().read(buffer)
            );
        }

        @Override
        public void handle(Packet message, NetworkEvent.Context context) {
            getInstance().requestedWaystoneAtLocationEventDispatcher.dispatch(message, false);
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
        public void encode(Packet message, FriendlyByteBuf buffer) {
            StringSerializer.instance.write(message.name, buffer);
        }

        @Override
        public Packet decode(FriendlyByteBuf buffer) {
            return new Packet(StringSerializer.instance.read(buffer));
        }

        @Override
        public void handle(Packet message, NetworkEvent.Context context) {
            Optional<WaystoneLocationData> dataAt = getInstance().getByName(message.name).map(e -> e.getValue().locationData);
            PacketHandler.send(
                PacketDistributor.PLAYER.with(context::getSender),
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
        public void encode(Packet message, FriendlyByteBuf buffer) {
            StringSerializer.instance.write(message.name, buffer);
            WaystoneLocationData.SERIALIZER.optional().write(message.data, buffer);
        }

        @Override
        public Packet decode(FriendlyByteBuf buffer) {
            return new Packet(
                StringSerializer.instance.read(buffer),
                WaystoneLocationData.SERIALIZER.optional().read(buffer)
            );
        }

        @Override
        public void handle(Packet message, NetworkEvent.Context context) {
            getInstance().requestedWaystoneLocationEventDispatcher.dispatch(message, false);
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
        public void encode(Packet message, FriendlyByteBuf buffer) {
            StringSerializer.instance.write(message.name, buffer);
        }

        @Override
        public Packet decode(FriendlyByteBuf buffer) {
            return new Packet(StringSerializer.instance.read(buffer));
        }

        @Override
        public void handle(Packet message, NetworkEvent.Context context) {
            PacketHandler.send(
                PacketDistributor.PLAYER.with(context::getSender),
                new DeliverIdEvent.Packet(getInstance().getHandleFor(message.name)));
        }

    }

    private static final class DeliverIdEvent implements PacketHandler.Event<DeliverIdEvent.Packet> {

        private static final class Packet {
            private final Optional<WaystoneHandle.Vanilla> waystone;
            private Packet(Optional<WaystoneHandle.Vanilla> waystone) {
                this.waystone = waystone;
            }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, FriendlyByteBuf buffer) {
           WaystoneHandle.Vanilla.Serializer.optional().write(message.waystone, buffer);
        }

        @Override
        public Packet decode(FriendlyByteBuf buffer) {
            return new Packet(WaystoneHandle.Vanilla.Serializer.optional().read(buffer));
        }

        @Override
        public void handle(Packet message, NetworkEvent.Context context) {
            getInstance().requestedIdEventDispatcher.dispatch(message.waystone, true);
        }

    }

    public CompoundTag saveTo(CompoundTag compound) {
        ListTag waystones = new ListTag();
        waystones.addAll(
            allWaystones.entrySet().stream().map(entry -> {
                CompoundTag entryCompound = new CompoundTag();
                entryCompound.put("Waystone", WaystoneHandle.Vanilla.Serializer.write(entry.getKey()));
                entryCompound.putString("Name", entry.getValue().name);
                entryCompound.put("Location", WaystoneLocationData.SERIALIZER.write(entry.getValue().locationData));
                entryCompound.putBoolean("IsLocked", entry.getValue().isLocked);
                return entryCompound;
            }).collect(Collectors.toSet()));
        compound.put("Waystones", waystones);

        ListTag memory = new ListTag();
        memory.addAll(
            playerMemory.entrySet().stream().map(entry -> {
                CompoundTag entryCompound = new CompoundTag();
                entryCompound.putUUID("Player", entry.getKey().id);
                ListTag known = new ListTag();
                known.addAll(entry.getValue().stream().map(WaystoneHandle.Vanilla.Serializer::write).collect(Collectors.toSet()));
                entryCompound.put("DiscoveredWaystones", known);
                return entryCompound;
            }).collect(Collectors.toSet())
        );
        compound.put("PlayerMemory", memory);
        return compound;
    }

    public void readFrom(CompoundTag compound) {
        allWaystones.clear();
        Tag dynamicWaystones = compound.get("Waystones");
        if(dynamicWaystones instanceof ListTag) {
            for(Tag dynamicEntry : ((ListTag) dynamicWaystones)) {
                if(dynamicEntry instanceof CompoundTag) {
                    CompoundTag entry = (CompoundTag) dynamicEntry;
                    WaystoneHandle.Vanilla waystone = WaystoneHandle.Vanilla.Serializer.read(entry.getCompound("Waystone"));
                    String name = entry.getString("Name");
                    WaystoneLocationData location = WaystoneLocationData.SERIALIZER.read(entry.getCompound("Location"));
                    boolean isLocked = entry.getBoolean("IsLocked");
                    allWaystones.put(waystone, new WaystoneEntry(name, location, isLocked));
                }
            }
        }

        playerMemory.clear();
        Tag dynamicPlayerMemory = compound.get("PlayerMemory");
        if(dynamicPlayerMemory instanceof ListTag) {
            for(Tag dynamicEntry : ((ListTag) dynamicPlayerMemory)) {
                if (dynamicEntry instanceof CompoundTag) {
                    CompoundTag entry = (CompoundTag) dynamicEntry;
                    UUID player = entry.getUUID("Player");
                    Tag dynamicKnown = entry.get("DiscoveredWaystones");
                    Set<WaystoneHandle.Vanilla> known = dynamicKnown instanceof ListTag
                        ?  ((ListTag) dynamicKnown).stream()
                            .filter(e -> e instanceof CompoundTag)
                            .map(e -> WaystoneHandle.Vanilla.Serializer.read((CompoundTag) e))
                            .collect(Collectors.toSet())
                        : new HashSet<>();
                    playerMemory.put(new PlayerHandle(player), known);
                }
            }
        }
    }

}