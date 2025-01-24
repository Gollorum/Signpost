package gollorum.signpost;

import gollorum.signpost.blockpartdata.types.renderers.BlockPartWaystoneUpdateListener;
import gollorum.signpost.compat.ExternalWaystoneLibrary;
import gollorum.signpost.events.*;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.minecraft.storage.WaystoneLibraryStorage;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.mixin.LevelAccessor;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.*;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.StringSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
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

    // Server only
    private SavedData savedData;
    public boolean hasStorageBeenSetup() { return savedData != null; }

    public static void initialize() {
        instance = new WaystoneLibrary();
        BlockPartWaystoneUpdateListener.getInstance().initialize();
    }

    private final EventDispatcher.Impl.WithPublicDispatch<WaystoneUpdatedEvent> _updateEventDispatcher = new EventDispatcher.Impl.WithPublicDispatch<>();

    public final EventDispatcher<WaystoneUpdatedEvent> updateEventDispatcher = _updateEventDispatcher;

    public static void registerNetworkPackets() {
        PacketHandler.onInitializeDo(packetHandler -> {
            packetHandler.register(new RequestAllWaystoneNamesEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "request_all_waystone_names"));
            packetHandler.register(new DeliverAllWaystoneNamesEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "deliver_all_waystone_names"));
            packetHandler.register(new RequestAllWaystonesEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "request_all_waystones"));
            packetHandler.register(new DeliverAllWaystonesEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "deliver_all_waystones"));
            packetHandler.register(new WaystoneUpdatedEventEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "waystone_updated_event"));
            packetHandler.register(new RequestWaystoneLocationEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "request_waystone_location"));
            packetHandler.register(new DeliverWaystoneLocationEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "deliver_waystone_location"));
            packetHandler.register(new RequestWaystoneAtLocationEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "request_waystone_at_location"));
            packetHandler.register(new DeliverWaystoneAtLocationEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "deliver_waystone_at_location"));
            packetHandler.register(new DeliverIdEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "deliver_id"));
            packetHandler.register(new RequestIdEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "request_id"));
            return true;
        });
    }

    public void setupStorage(ServerLevel world){
        DimensionDataStorage storage = world.getDataStorage();
        savedData = storage.computeIfAbsent(
            new SavedData.Factory<>(
                WaystoneLibraryStorage::new,
                (tag, provider) -> new WaystoneLibraryStorage().load(tag, provider),
                DataFixTypes.SAVED_DATA_MAP_DATA),
            WaystoneLibraryStorage.NAME);
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
        PacketHandler.getInstance().sendToServer(new WaystoneUpdatedEventEvent.Packet(WaystoneUpdatedEvent.fromUpdated(location, newName, isLocked, WaystoneHandle.Vanilla.NIL)));
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
                // This should not happen unless a sender tries to hacc
                editingPlayer.displayClientMessage(Component.translatable(LangKeys.noPermissionWaystone), false);
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
        PacketHandler.getInstance().sendToAll(new WaystoneUpdatedEventEvent.Packet(updatedEvent));
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
        PacketHandler.getInstance().sendToAll(new WaystoneUpdatedEventEvent.Packet(updatedEvent));
        markDirty();
        WaystoneBlock.discover(PlayerHandle.from(editingPlayer), new WaystoneData(id, newName, location, isLocked));
        return true;
    }

    private boolean validateHandleDoesNotExist(WaystoneHandle.Vanilla handle, Player editingPlayer) {
        if(allWaystones.containsKey(handle)) {
            editingPlayer.displayClientMessage(Component.translatable(LangKeys.duplicateWaystoneId), false);
            return false;
        } else return true;
    }

    private boolean validateNameDoesNotExist(String newName, @Nullable Player editingPlayer) {
        if(allWaystones.values().stream().anyMatch(entry -> entry.name.equals(newName))) {
            if(editingPlayer != null)
                editingPlayer.displayClientMessage(Component.translatable(LangKeys.duplicateWaystoneName, newName), true);
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
            PacketHandler.getInstance().sendToAll(new WaystoneUpdatedEventEvent.Packet(new WaystoneRemovedEvent(oldEntry.locationData, oldEntry.name, handle)));
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
            PacketHandler.getInstance().sendToServer(new RequestAllWaystoneNamesEvent.Packet(onlyKnownBy));
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
            PacketHandler.getInstance().sendToServer(new RequestAllWaystonesEvent.Packet(onlyKnownBy));
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
            PacketHandler.getInstance().sendToServer(new RequestWaystoneAtLocationEvent.Packet(location));
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
        if(onlyKnownBy.isPresent() && IConfig.IServer.getInstance().teleport().enforceDiscovery()) {
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
            .collect(Collectors.toMap(Map.Entry::getKey, e -> Tuple.of(e.getValue().name, e.getValue().locationData.withoutExplicitLevel())));
        if(onlyKnownBy.isPresent() && IConfig.IServer.getInstance().teleport().enforceDiscovery()) {
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

    private final long tileEntityExistenceCheckCooldownMillis = 1000 * 60 * 10;
    private final Map<ResourceLocation, Map<BlockPos, Long>> checkedTileEntities = new HashMap<>();

    private boolean assertTileEntityExists(WaystoneEntry entry) {
        var cache = checkedTileEntities.computeIfAbsent(
            entry.locationData.block.world.rightOr(l -> l.dimension().location()),
            key -> new HashMap<>()
        );
        var time = System.currentTimeMillis();
        var blockPos = entry.locationData.block.blockPos;
        var lastChecked = cache.get(blockPos);
        if(lastChecked != null && lastChecked + tileEntityExistenceCheckCooldownMillis >= time) {
            return true;
        }

        Optional<ServerLevel> level = TileEntityUtils.toWorld(entry.locationData.block.world, false)
            .flatMap(lv -> lv instanceof ServerLevel ? Optional.of((ServerLevel)lv) : Optional.empty());
        if(level.isEmpty()) return true; // Something is wrong, I cannot find the level to check.
        if(((LevelAccessor)level.get()).getThread() != Thread.currentThread()) { // Cannot check on wrong thread.
            IDelay.onServerForFrames(1, () -> checkEntity(level.get(), blockPos, cache));
            return true;
        } else return checkEntity(level.get(), blockPos, cache);
    }

    private static boolean checkEntity(ServerLevel level, BlockPos blockPos, Map<BlockPos, Long> cache) {
        Optional entity = level.getBlockEntity(blockPos, WaystoneTile.getBlockEntityType());
        if(entity.isEmpty()) entity = level.getBlockEntity(blockPos, PostTile.getBlockEntityType());
        if(entity.isPresent()) {
            cache.put(blockPos, System.currentTimeMillis());
            return true;
        }
        else {
            WaystoneTile.onRemoved(level, blockPos);
            return false;
        }
    }

    public void markDirty(){
        // savedData is null on dedicated clients
        if(savedData != null) savedData.setDirty();
    }

    private static final class RequestAllWaystoneNamesEvent implements PacketHandler.Event.ForServer<RequestAllWaystoneNamesEvent.Packet> {

        public static final class Packet {
            public final Optional<PlayerHandle> onlyKnownBy;

            public Packet(Optional<PlayerHandle> onlyKnownBy) { this.onlyKnownBy = onlyKnownBy; }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, Packet message) {
            PlayerHandle.BufferSerializer.optional().encode(buffer, message.onlyKnownBy);
        }

        @Override
        public Packet decode(RegistryFriendlyByteBuf buffer) {
            return new Packet(PlayerHandle.BufferSerializer.optional().decode(buffer));
        }

        @Override
        public void handle(Packet message, PacketHandler.Context.Server context) {
            PacketHandler.getInstance().sendToPlayer(
                context.sender(),
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
        public void encode(RegistryFriendlyByteBuf buffer, Packet message) {
            buffer.writeInt(message.names.size());
            for (Map.Entry<WaystoneHandle.Vanilla, String> name: message.names.entrySet()) {
                buffer.writeUUID(name.getKey().id);
                StringSerializer.Buffer.encode(buffer, name.getValue());
            }
        }

        @Override
        public Packet decode(RegistryFriendlyByteBuf buffer) {
            Map<WaystoneHandle.Vanilla, String> names = new HashMap<>();
            int count = buffer.readInt();
            for(int i = 0; i < count; i++)
                names.put(new WaystoneHandle.Vanilla(buffer.readUUID()), StringSerializer.Buffer.decode(buffer));
            return new Packet(names);
        }

        @Override
        public void handle(Packet message, PacketHandler.Context context) {
            getInstance().cachedWaystoneNames.clear();
            getInstance().cachedWaystoneNames.addAll(message.names.values());
            getInstance().isWaystoneNameCacheDirty = false;
            getInstance().requestedAllNamesEventDispatcher.dispatch(message.names, true);
        }
    }

    private static final class RequestAllWaystonesEvent implements PacketHandler.Event.ForServer<RequestAllWaystonesEvent.Packet> {

        private static final BufferSerializable<Optional<PlayerHandle>> serializer = PlayerHandle.BufferSerializer.optional();

        public static final class Packet {
            public final Optional<PlayerHandle> onlyKnownBy;

            public Packet(Optional<PlayerHandle> onlyKnownBy) {
                this.onlyKnownBy = onlyKnownBy;
            }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, Packet message) {
            serializer.encode(buffer, message.onlyKnownBy);
        }

        @Override
        public Packet decode(RegistryFriendlyByteBuf buffer) {
            return new Packet(serializer.decode(buffer));
        }

        @Override
        public void handle(Packet message, PacketHandler.Context.Server context) {
            PacketHandler.getInstance().sendToPlayer(
                context.sender(),
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
        public void encode(RegistryFriendlyByteBuf buffer, Packet message) {
            buffer.writeInt(message.data.size());
            for (Map.Entry<WaystoneHandle.Vanilla, Tuple<String, WaystoneLocationData>> name: message.data.entrySet()) {
                buffer.writeUUID(name.getKey().id);
                buffer.writeUtf(name.getValue()._1);
                WaystoneLocationData.BUFFER_SERIALIZER.encode(buffer, name.getValue()._2);
            }
        }

        @Override
        public Packet decode(RegistryFriendlyByteBuf buffer) {
            Map<WaystoneHandle.Vanilla, Tuple<String, WaystoneLocationData>> names = new HashMap<>();
            int count = buffer.readInt();
            for(int i = 0; i < count; i++)
                names.put(
                    new WaystoneHandle.Vanilla(buffer.readUUID()),
                    Tuple.of(
                        StringSerializer.Buffer.decode(buffer),
                        WaystoneLocationData.BUFFER_SERIALIZER.decode(buffer)
                    )
                );
            return new Packet(names);
        }

        @Override
        public void handle(Packet message, PacketHandler.Context context) {
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
        public void encode(RegistryFriendlyByteBuf buffer, Packet message) {
            WaystoneUpdatedEvent.Serializer.INSTANCE.encode(buffer, message.event);
        }

        @Override
        public Packet decode(RegistryFriendlyByteBuf buffer) {
            return new Packet(WaystoneUpdatedEvent.Serializer.INSTANCE.decode(buffer));
        }

        @Override
        public void handle(Packet message, PacketHandler.Context context) {
            if(context instanceof PacketHandler.Context.Server serverContext){
                Player player = serverContext.sender();
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

    private static final class RequestWaystoneAtLocationEvent implements PacketHandler.Event.ForServer<RequestWaystoneAtLocationEvent.Packet> {

        public static final class Packet {
            public final WorldLocation waystoneLocation;

            public Packet(WorldLocation waystoneLocation) { this.waystoneLocation = waystoneLocation.withoutExplicitLevel(); }
        }

        @Override
        public Class<Packet> getMessageClass() {
            return Packet.class;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, Packet message) {
            WorldLocation.BUFFER_SERIALIZER.encode(buffer, message.waystoneLocation);
        }

        @Override
        public Packet decode(RegistryFriendlyByteBuf buffer) {
            return new Packet(WorldLocation.BUFFER_SERIALIZER.decode(buffer));
        }

        @Override
        public void handle(Packet message, PacketHandler.Context.Server context) {
            Optional<WaystoneData> dataAt = getInstance().tryGetWaystoneDataAt(message.waystoneLocation);
            PacketHandler.getInstance().sendToPlayer(
                context.sender(),
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
                this.waystoneLocation = waystoneLocation.withoutExplicitLevel();
                this.data = data.map(WaystoneData::withoutExplicitLevel);
            }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, Packet message) {
            WorldLocation.BUFFER_SERIALIZER.encode(buffer, message.waystoneLocation);
            WaystoneData.BUFFER_SERIALIZER.optional().encode(buffer, message.data);
        }

        @Override
        public Packet decode(RegistryFriendlyByteBuf buffer) {
            return new Packet(
                WorldLocation.BUFFER_SERIALIZER.decode(buffer),
                WaystoneData.BUFFER_SERIALIZER.optional().decode(buffer)
            );
        }

        @Override
        public void handle(Packet message, PacketHandler.Context context) {
            getInstance().requestedWaystoneAtLocationEventDispatcher.dispatch(message, false);
        }

    }

    private static final class RequestWaystoneLocationEvent implements PacketHandler.Event.ForServer<RequestWaystoneLocationEvent.Packet> {

        public static final class Packet {
            public final String name;

            public Packet(String name) { this.name = name; }
        }

        @Override
        public Class<Packet> getMessageClass() {
            return Packet.class;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, Packet message) {
            StringSerializer.Buffer.encode(buffer, message.name);
        }

        @Override
        public Packet decode(RegistryFriendlyByteBuf buffer) {
            return new Packet(StringSerializer.Buffer.decode(buffer));
        }

        @Override
        public void handle(Packet message, PacketHandler.Context.Server context) {
            Optional<WaystoneLocationData> dataAt = getInstance().getByName(message.name).map(e -> e.getValue().locationData);
            PacketHandler.getInstance().sendToPlayer(
                context.sender(),
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
                this.data = data.map(WaystoneLocationData::withoutExplicitLevel);
            }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, Packet message) {
            StringSerializer.Buffer.encode(buffer, message.name);
            WaystoneLocationData.BUFFER_SERIALIZER.optional().encode(buffer, message.data);
        }

        @Override
        public Packet decode(RegistryFriendlyByteBuf buffer) {
            return new Packet(
                StringSerializer.Buffer.decode(buffer),
                WaystoneLocationData.BUFFER_SERIALIZER.optional().decode(buffer)
            );
        }

        @Override
        public void handle(Packet message, PacketHandler.Context context) {
            getInstance().requestedWaystoneLocationEventDispatcher.dispatch(message, false);
        }

    }

    private static final class RequestIdEvent implements PacketHandler.Event.ForServer<RequestIdEvent.Packet> {

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
        public void encode(RegistryFriendlyByteBuf buffer, Packet message) {
            StringSerializer.Buffer.encode(buffer, message.name);
        }

        @Override
        public Packet decode(RegistryFriendlyByteBuf buffer) {
            return new Packet(StringSerializer.Buffer.decode(buffer));
        }

        @Override
        public void handle(Packet message, PacketHandler.Context.Server context) {
            PacketHandler.getInstance().sendToPlayer(
                context.sender(),
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
        public void encode(RegistryFriendlyByteBuf buffer, Packet message) {
           WaystoneHandle.Vanilla.BufferSerializer.optional().encode(buffer, message.waystone);
        }

        @Override
        public Packet decode(RegistryFriendlyByteBuf buffer) {
            return new Packet(WaystoneHandle.Vanilla.BufferSerializer.optional().decode(buffer));
        }

        @Override
        public void handle(Packet message, PacketHandler.Context context) {
            getInstance().requestedIdEventDispatcher.dispatch(message.waystone, true);
        }

    }

    public CompoundTag saveTo(CompoundTag compound, HolderLookup.Provider registryAccess) {
        ListTag waystones = new ListTag();
        waystones.addAll(
            allWaystones.entrySet().stream().map(entry -> {
                CompoundTag entryCompound = new CompoundTag();
                entryCompound.put("Waystone", WaystoneHandle.Vanilla.CompoundSerializer.encode(entry.getKey(), registryAccess));
                entryCompound.putString("Name", entry.getValue().name);
                entryCompound.put("Location", WaystoneLocationData.COMPOUND_SERIALIZER.encode(entry.getValue().locationData, registryAccess));
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
                known.addAll(entry.getValue().stream().map(t ->
                    WaystoneHandle.Vanilla.CompoundSerializer.encode(t, registryAccess)).collect(Collectors.toSet()));
                entryCompound.put("DiscoveredWaystones", known);
                return entryCompound;
            }).collect(Collectors.toSet())
        );
        compound.put("PlayerMemory", memory);
        return compound;
    }

    public void readFrom(CompoundTag compound, HolderLookup.Provider registryAccess) {
        allWaystones.clear();
        Tag dynamicWaystones = compound.get("Waystones");
        if(dynamicWaystones instanceof ListTag) {
            for(Tag dynamicEntry : ((ListTag) dynamicWaystones)) {
                if(dynamicEntry instanceof CompoundTag) {
                    CompoundTag entry = (CompoundTag) dynamicEntry;
                    WaystoneHandle.Vanilla waystone = WaystoneHandle.Vanilla.CompoundSerializer.decode(entry.getCompound("Waystone"), registryAccess);
                    String name = entry.getString("Name");
                    WaystoneLocationData location = WaystoneLocationData.COMPOUND_SERIALIZER.decode(entry.getCompound("Location"), registryAccess);
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
                            .map(e -> WaystoneHandle.Vanilla.CompoundSerializer.decode((CompoundTag) e, registryAccess))
                            .collect(Collectors.toSet())
                        : new HashSet<>();
                    playerMemory.put(new PlayerHandle(player), known);
                }
            }
        }
    }

}