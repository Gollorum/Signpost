package gollorum.signpost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import gollorum.signpost.minecraft.worldgen.VillageWaystone;
import gollorum.signpost.mixin.LevelAccessor;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.*;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class WaystoneLibrary {

    private static WaystoneLibrary instance;

    public static WaystoneLibrary getInstance() {
        if(instance == null) {
            if (Signpost.getServerType().isServer) {
                initializeServer(Signpost.getServerInstance().overworld());
            } else {
                initializeClient();
            }
            Signpost.LOGGER.warn("Force-initialized waystone library. This should not happen.");
        }
        return instance;
    }

    public static boolean hasInstance() { return instance != null; }

    public static void initializeServer(ServerLevel overworld) {
        var data = overworld.getDataStorage().computeIfAbsent(WaystoneLibraryStorage.TYPE);
        instance = new WaystoneLibrary(data);
        BlockPartWaystoneUpdateListener.getInstance().initialize();
    }

    public static void initializeClient() {
        var data = new WaystoneLibraryStorage(new HashMap<>(), new HashMap<>(), new VillageWaystone());
        instance = new WaystoneLibrary(data);
        BlockPartWaystoneUpdateListener.getInstance().initialize();
    }

    public final WaystoneLibraryStorage data;
    public VillageWaystone getVillageWaystones() {
        return data.villageWaystones;
    }

    private WaystoneLibrary(WaystoneLibraryStorage data) {
        this.data = data;
        
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
            data.setDirty();
        });
    }

    private final EventDispatcher.Impl.WithPublicDispatch<WaystoneUpdatedEvent> _updateEventDispatcher = new EventDispatcher.Impl.WithPublicDispatch<>();

    public final EventDispatcher<WaystoneUpdatedEvent> updateEventDispatcher = _updateEventDispatcher;

    public WaystoneLocationData getLocationData(WaystoneHandle.Vanilla waystoneId) {
        assert Signpost.getServerType().isServer;
        return data.allWaystones.get(waystoneId).locationData;
    }

    public Optional<WaystoneDataBase> getData(WaystoneHandle handle) {
        return handle instanceof WaystoneHandle.Vanilla
            ? getData((WaystoneHandle.Vanilla) handle).map(d -> d)
            : ExternalWaystoneLibrary.getInstance().getData(handle).map(d -> d);
    }

    public Optional<WaystoneData> getData(WaystoneHandle.Vanilla waystoneId) {
        assert Signpost.getServerType().isServer;
        WaystoneEntry entry = data.allWaystones.get(waystoneId);
        return entry == null
            ? Optional.empty()
            : Optional.of(new WaystoneData(waystoneId, entry.name, entry.locationData, entry.isLocked));
    }

    public record WaystoneEntry(String name, WaystoneLocationData locationData, boolean isLocked) {

        public boolean hasThePermissionToEdit(Player player) {
            return WaystoneData.hasThePermissionToEdit(player, locationData, isLocked);
        }

        public static final MapCodec<WaystoneEntry> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                Codec.STRING.fieldOf("Name").forGetter(WaystoneEntry::name),
                WaystoneLocationData.CODEC.fieldOf("Location").forGetter(WaystoneEntry::locationData),
                Codec.BOOL.fieldOf("IsLocked").forGetter(WaystoneEntry::isLocked)
            ).apply(instance, WaystoneEntry::new));

    }

    public record WaystoneInfo(String name, WaystoneLocationData locationData, WaystoneHandle.Vanilla handle) { }

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

    public Optional<String> update(String newName, WaystoneLocationData location, Player editingPlayer, boolean isLocked) {
        assert Signpost.getServerType().isServer && location.block().world().match(w -> (w instanceof ServerLevel), i -> true);
        WaystoneHandle.Vanilla[] oldWaystones = data.allWaystones
            .entrySet()
            .stream()
            .filter(e -> e.getValue().locationData.block().equals(location.block()))
            .map(Map.Entry::getKey)
            .distinct()
            .toArray(WaystoneHandle.Vanilla[]::new);
        String[] oldNames = Arrays.stream(oldWaystones).map(id -> data.allWaystones.get(id).name).toArray(String[]::new);
        if(oldWaystones.length > 1)
            Signpost.LOGGER.error("Waystone at " + location + " (new name: " + newName +") was already present "
                + oldWaystones.length + " times. This indicates invalid state. Names found: " + String.join(", ", oldNames));
        if(oldWaystones.length > 0) {
            WaystoneEntry oldEntry = data.allWaystones.get(oldWaystones[0]);
            if(editingPlayer != null && !oldEntry.hasThePermissionToEdit(editingPlayer)) {
                // This should not happen unless a sender tries to hacc
                editingPlayer.displayClientMessage(Component.translatable(LangKeys.noPermissionWaystone), false);
                return Optional.empty();
            }
            if(editingPlayer != null && !gollorum.signpost.utils.WaystoneData.hasSecurityPermissions(editingPlayer, location))
                isLocked = oldEntry.isLocked;
            for(WaystoneHandle.Vanilla oldId: oldWaystones) {
                data.allWaystones.remove(oldId);
            }
        }
        if(!validateNameDoesNotExist(newName, editingPlayer)) return Optional.empty();
        WaystoneHandle.Vanilla id = oldWaystones.length > 0 ? oldWaystones[0] : new WaystoneHandle.Vanilla(UUID.randomUUID());
        data.allWaystones.put(id, new WaystoneEntry(newName, location, isLocked));
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
        if(data.allWaystones.values().stream().anyMatch(entry -> entry.locationData.block().equals(location.block()))) {
            Signpost.LOGGER.error("Waystone at " + location + " (new name: " + newName +") was already present. " +
                "This indicates invalid state.");
            return false;
        }
        WaystoneHandle.Vanilla id = handle.orElseGet(() -> new WaystoneHandle.Vanilla(UUID.randomUUID()));
        boolean isLocked = false;
        data.allWaystones.put(id, new WaystoneEntry(newName, location, isLocked));
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
        if(data.allWaystones.containsKey(handle)) {
            editingPlayer.displayClientMessage(Component.translatable(LangKeys.duplicateWaystoneId), false);
            return false;
        } else return true;
    }

    private boolean validateNameDoesNotExist(String newName, Player editingPlayer) {
        if(data.allWaystones.values().stream().anyMatch(entry -> entry.name.equals(newName))) {
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
        WaystoneEntry oldEntry = data.allWaystones.remove(handle);
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
            data.allWaystones.remove(oldEntry.get().getKey());
            Vector3 newSpawnLocation = oldEntry.get().getValue().locationData.spawn()
                .add(Vector3.fromBlockPos(newLocation.blockPos().subtract(oldLocation.blockPos())));
            data.allWaystones.put(oldEntry.get().getKey(), new WaystoneEntry(oldEntry.get().getValue().name, new WaystoneLocationData(newLocation, newSpawnLocation),
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
        return data.allWaystones.entrySet().stream()
            .filter(e -> e.getValue().name.equals(name)).findFirst();
    }

    private Optional<Map.Entry<WaystoneHandle.Vanilla, WaystoneEntry>> getByLocation(WorldLocation location){
        assert Signpost.getServerType().isServer;
        return data.allWaystones.entrySet().stream()
            .filter(e -> e.getValue().locationData.block().equals(location)).findFirst();
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
            PacketHandler.getInstance().sendToServer(RequestWaystoneAtLocationEvent.Packet.from(location));
        } else onReply.accept(tryGetWaystoneDataAt(location));
    }

    private Optional<WaystoneHandle.Vanilla> getHandleFor(String name){
        return data.allWaystones.entrySet().stream()
            .filter(e -> e.getValue().name.equals(name))
            .map(Map.Entry::getKey)
            .findFirst();
    }

    private Map<WaystoneHandle.Vanilla, String> getAllWaystoneNamesAndHandles(Optional<PlayerHandle> onlyKnownBy) {
        assert Signpost.getServerType().isServer;
        Map<WaystoneHandle.Vanilla, String> ret = getInstance().data.allWaystones.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().name));
        if(isWaystoneNameCacheDirty) {
            cachedWaystoneNames.clear();
            cachedWaystoneNames.addAll(ret.values());
            isWaystoneNameCacheDirty = false;
        }
        if(onlyKnownBy.isPresent() && IConfig.IServer.getInstance().teleport().enforceDiscovery()) {
            Set<WaystoneHandle.Vanilla> known = data.playerMemory.computeIfAbsent(onlyKnownBy.get(), h -> new HashSet<>());
            return ret.entrySet().stream()
                .filter(e -> known.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return ret;
    }

    private Map<WaystoneHandle.Vanilla, Tuple<String, WaystoneLocationData>> getAllWaystones(Optional<PlayerHandle> onlyKnownBy) {
        assert Signpost.getServerType().isServer;
        Map<WaystoneHandle.Vanilla, Tuple<String, WaystoneLocationData>> ret = getInstance().data.allWaystones.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> Tuple.of(e.getValue().name, e.getValue().locationData.withoutExplicitLevel())));
        if(onlyKnownBy.isPresent() && IConfig.IServer.getInstance().teleport().enforceDiscovery()) {
            PlayerHandle player = onlyKnownBy.get();
            Set<WaystoneHandle.Vanilla> known = data.playerMemory.computeIfAbsent(player, h -> new HashSet<>());
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
        return data.allWaystones.entrySet().stream().map(entry -> new WaystoneInfo(
            entry.getValue().name,
            entry.getValue().locationData,
            entry.getKey()
        )).collect(Collectors.toSet());
    }

    private Optional<WaystoneData> tryGetWaystoneDataAt(WorldLocation location) {
        assert Signpost.getServerType().isServer;
        return getInstance().data.allWaystones.entrySet().stream()
            .filter(e -> e.getValue().locationData.block().equals(location))
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
        if(data.playerMemory.computeIfAbsent(player, p -> new HashSet<>()).add(waystone)) {
            markDirty();
            return true;
        } return false;
    }

    public boolean isDiscovered(PlayerHandle player, WaystoneHandle.Vanilla waystone) {
        if(!data.playerMemory.containsKey(player))
            data.playerMemory.put(player, new HashSet<>());
        return data.playerMemory.get(player).contains(waystone);
    }

    public boolean contains(WaystoneHandle.Vanilla waystone) {
        assert Signpost.getServerType().isServer;
        if(!data.allWaystones.containsKey(waystone)) return false;
        WaystoneEntry entry = data.allWaystones.get(waystone);
        return assertTileEntityExists(entry);
    }

    private final long tileEntityExistenceCheckCooldownMillis = 1000 * 60 * 10;
    private final Map<ResourceLocation, Map<BlockPos, Long>> checkedTileEntities = new HashMap<>();

    private boolean assertTileEntityExists(WaystoneEntry entry) {
        var cache = checkedTileEntities.computeIfAbsent(
            entry.locationData.block().world().rightOr(l -> l.dimension().location()),
            key -> new HashMap<>()
        );
        var time = System.currentTimeMillis();
        var blockPos = entry.locationData.block().blockPos();
        var lastChecked = cache.get(blockPos);
        if(lastChecked != null && lastChecked + tileEntityExistenceCheckCooldownMillis >= time) {
            return true;
        }

        Optional<ServerLevel> level = TileEntityUtils.toWorld(entry.locationData.block().world(), false)
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
        data.setDirty();
    }

    public static final class RequestAllWaystoneNamesEvent implements PacketHandler.Event.ForServer<RequestAllWaystoneNamesEvent.Packet> {

        public record Packet(Optional<PlayerHandle> onlyKnownBy) {

            public static final StreamCodec<RegistryFriendlyByteBuf, Packet> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(PlayerHandle.STREAM_CODEC), Packet::onlyKnownBy,
                Packet::new
            );
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Packet> codec() {
            return Packet.STREAM_CODEC;
        }

        @Override
        public void handle(Packet message, PacketHandler.Context.Server context) {
            PacketHandler.getInstance().sendToPlayer(
                context.sender(),
                new DeliverAllWaystoneNamesEvent.Packet(getInstance().getAllWaystoneNamesAndHandles(message.onlyKnownBy))
            );
        }

    }

    public static final class DeliverAllWaystoneNamesEvent implements PacketHandler.Event<DeliverAllWaystoneNamesEvent.Packet> {

        public static final record Packet(Map<WaystoneHandle.Vanilla, String> names) {
            public static final StreamCodec<RegistryFriendlyByteBuf, Packet> STREAM_CODEC = ByteBufCodecs.<RegistryFriendlyByteBuf, WaystoneHandle.Vanilla, String, Map<WaystoneHandle.Vanilla, String>>map(
                HashMap::new,
                WaystoneHandle.Vanilla.STREAM_CODEC,
                ByteBufCodecs.STRING_UTF8
            ).map(Packet::new, Packet::names);
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Packet> codec() {
            return Packet.STREAM_CODEC;
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void handle(Packet message, PacketHandler.Context context) {
            getInstance().cachedWaystoneNames.clear();
            getInstance().cachedWaystoneNames.addAll(message.names.values());
            getInstance().isWaystoneNameCacheDirty = false;
            getInstance().requestedAllNamesEventDispatcher.dispatch(message.names, true);
        }
    }

    public static final class RequestAllWaystonesEvent implements PacketHandler.Event.ForServer<RequestAllWaystonesEvent.Packet> {

        public record Packet(Optional<PlayerHandle> onlyKnownBy) {
            public static final StreamCodec<RegistryFriendlyByteBuf, Packet> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(PlayerHandle.STREAM_CODEC), Packet::onlyKnownBy,
                Packet::new
            );
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Packet> codec() {
            return Packet.STREAM_CODEC;
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void handle(Packet message, PacketHandler.Context.Server context) {
            PacketHandler.getInstance().sendToPlayer(
                context.sender(),
                new DeliverAllWaystonesEvent.Packet(getInstance().getAllWaystones(message.onlyKnownBy))
            );
        }

    }

    public static final class DeliverAllWaystonesEvent implements PacketHandler.Event<DeliverAllWaystonesEvent.Packet> {

        public static final record Packet(Map<WaystoneHandle.Vanilla, Tuple<String, WaystoneLocationData>> data) {
            public static final StreamCodec<RegistryFriendlyByteBuf, Packet> STREAM_CODEC = ByteBufCodecs.<RegistryFriendlyByteBuf, WaystoneHandle.Vanilla, Tuple<String, WaystoneLocationData>, Map<WaystoneHandle.Vanilla, Tuple<String, WaystoneLocationData>>>map(
                HashMap::new,
                WaystoneHandle.Vanilla.STREAM_CODEC,
                Tuple.streamCodec(ByteBufCodecs.STRING_UTF8, WaystoneLocationData.STREAM_CODEC)
            ).map(Packet::new, Packet::data);
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Packet> codec() {
            return Packet.STREAM_CODEC;
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void handle(Packet message, PacketHandler.Context context) {
            getInstance().requestedAllWaystonesEventDispatcher.dispatch(message.data, true);
        }
    }

    public static final class WaystoneUpdatedEventEvent implements PacketHandler.Event<WaystoneUpdatedEventEvent.Packet> {

        public static final record Packet(WaystoneUpdatedEvent event) {
            public static final StreamCodec<RegistryFriendlyByteBuf, Packet> STREAM_CODEC =
                WaystoneUpdatedEvent.Serializer.INSTANCE
                    .map(Packet::new, Packet::event)
                    .mapStream(b -> b);
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Packet> codec() {
            return Packet.STREAM_CODEC;
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void handle(Packet message, PacketHandler.Context context) {
            if(context instanceof PacketHandler.Context.Server serverContext){
                Player player = serverContext.sender();
                switch (message.event.getType()){
                    case Added:
                        if(!TileEntityUtils.findTileEntityAt(message.event.location.block(), WaystoneContainer.class, false).isPresent()) {
                            Signpost.LOGGER.error("Tried to add a waystone where no compatible TileEntity was present: " + message.event.location.block());
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
                            message.event.location.block(),
                            ((WaystoneMovedEvent)message.event).newLocation
                        );
                    default: throw new RuntimeException("Type " + message.event.getType() + " is not supported");
                }
            } else getInstance()._updateEventDispatcher.dispatch(message.event, false);
        }

    }

    public static final class RequestWaystoneAtLocationEvent implements PacketHandler.Event.ForServer<RequestWaystoneAtLocationEvent.Packet> {

        public record Packet(WorldLocation waystoneLocation) {
            public static Packet from(WorldLocation waystoneLocation) {
                return new Packet(waystoneLocation.withoutExplicitLevel());
            }
            
            public static final StreamCodec<RegistryFriendlyByteBuf, Packet> STREAM_CODEC = WorldLocation.STREAM_CODEC
                .map(Packet::new, Packet::waystoneLocation)
                .mapStream(b -> b);
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Packet> codec() {
            return Packet.STREAM_CODEC;
        }

        @Override
        public Class<Packet> getMessageClass() {
            return Packet.class;
        }

        @Override
        public void handle(Packet message, PacketHandler.Context.Server context) {
            Optional<WaystoneData> dataAt = getInstance().tryGetWaystoneDataAt(message.waystoneLocation);
            PacketHandler.getInstance().sendToPlayer(
                context.sender(),
                DeliverWaystoneAtLocationEvent.Packet.from(
                    message.waystoneLocation,
                    dataAt
                ));
        }

    }

    public static final class DeliverWaystoneAtLocationEvent implements PacketHandler.Event<DeliverWaystoneAtLocationEvent.Packet> {

        private record Packet(WorldLocation waystoneLocation, Optional<WaystoneData> data) {
            private static Packet from(WorldLocation waystoneLocation, Optional<WaystoneData> data) {
                return new Packet(
                    waystoneLocation.withoutExplicitLevel(),
                    data.map(WaystoneData::withoutExplicitLevel)
                );
            }
            
            public static final StreamCodec<RegistryFriendlyByteBuf, Packet> STREAM_CODEC = StreamCodec.composite(
                WorldLocation.STREAM_CODEC, Packet::waystoneLocation,
                ByteBufCodecs.optional(WaystoneData.STREAM_CODEC), Packet::data,
                Packet::new
            );
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Packet> codec() {
            return Packet.STREAM_CODEC;
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void handle(Packet message, PacketHandler.Context context) {
            getInstance().requestedWaystoneAtLocationEventDispatcher.dispatch(message, false);
        }

    }

    public static final class RequestWaystoneLocationEvent implements PacketHandler.Event.ForServer<RequestWaystoneLocationEvent.Packet> {

        public record Packet(String name) {
            public static final StreamCodec<RegistryFriendlyByteBuf, Packet> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
                .map(Packet::new, Packet::name)
                .mapStream(b -> b);
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Packet> codec() {
            return Packet.STREAM_CODEC;
        }

        @Override
        public Class<Packet> getMessageClass() {
            return Packet.class;
        }

        @Override
        public void handle(Packet message, PacketHandler.Context.Server context) {
            Optional<WaystoneLocationData> dataAt = getInstance().getByName(message.name).map(e -> e.getValue().locationData);
            PacketHandler.getInstance().sendToPlayer(
                context.sender(),
                DeliverWaystoneLocationEvent.Packet.from(
                    message.name,
                    dataAt
                ));
        }

    }

    public static final class DeliverWaystoneLocationEvent implements PacketHandler.Event<DeliverWaystoneLocationEvent.Packet> {

        private record Packet(String name, Optional<WaystoneLocationData> data) {
            private static Packet from(String name, Optional<WaystoneLocationData> data) {
                return new Packet(
                    name,
                    data.map(WaystoneLocationData::withoutExplicitLevel)
                );
            }
            public static final StreamCodec<RegistryFriendlyByteBuf, Packet> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Packet::name,
                ByteBufCodecs.optional(WaystoneLocationData.STREAM_CODEC), Packet::data,
                Packet::new
            );
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Packet> codec() {
            return Packet.STREAM_CODEC;
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void handle(Packet message, PacketHandler.Context context) {
            getInstance().requestedWaystoneLocationEventDispatcher.dispatch(message, false);
        }

    }

    public static final class RequestIdEvent implements PacketHandler.Event.ForServer<RequestIdEvent.Packet> {

        public record Packet(String name) {
            public static final StreamCodec<RegistryFriendlyByteBuf, Packet> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
                .map(Packet::new, Packet::name)
                .mapStream(b -> b);
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Packet> codec() {
            return Packet.STREAM_CODEC;
        }

        @Override
        public Class<Packet> getMessageClass() {
            return Packet.class;
        }

        @Override
        public void handle(Packet message, PacketHandler.Context.Server context) {
            PacketHandler.getInstance().sendToPlayer(
                context.sender(),
                new DeliverIdEvent.Packet(getInstance().getHandleFor(message.name)));
        }

    }

    public static final class DeliverIdEvent implements PacketHandler.Event<DeliverIdEvent.Packet> {

        private record Packet(Optional<WaystoneHandle.Vanilla> waystone) {
            public static final StreamCodec<RegistryFriendlyByteBuf, Packet> STREAM_CODEC = ByteBufCodecs.optional(WaystoneHandle.Vanilla.STREAM_CODEC)
                .map(Packet::new, Packet::waystone)
                .mapStream(b -> b);
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Packet> codec() {
            return Packet.STREAM_CODEC;
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void handle(Packet message, PacketHandler.Context context) {
            getInstance().requestedIdEventDispatcher.dispatch(message.waystone, true);
        }
    }
}