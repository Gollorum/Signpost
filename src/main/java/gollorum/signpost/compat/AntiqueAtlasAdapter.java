package gollorum.signpost.compat;

import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.events.WaystoneMovedEvent;
import gollorum.signpost.minecraft.events.WaystoneUpdatedEvent;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.Delay;
import gollorum.signpost.utils.WorldLocation;
import hunternif.mc.api.AtlasAPI;
import hunternif.mc.impl.atlas.AntiqueAtlasMod;
import hunternif.mc.impl.atlas.event.MarkerClickedCallback;
import hunternif.mc.impl.atlas.item.AntiqueAtlasItems;
import hunternif.mc.impl.atlas.marker.DimensionMarkersData;
import hunternif.mc.impl.atlas.marker.GlobalMarkersData;
import hunternif.mc.impl.atlas.marker.Marker;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class AntiqueAtlasAdapter {

    public static final ResourceLocation markerType = new ResourceLocation(Signpost.MOD_ID, "signpost");

    private static Storage storage = null;

    public static void register() {
        Signpost.onServerOverworldLoad.addListener(AntiqueAtlasAdapter::onWorldLoaded);
        MarkerClickedCallback.EVENT.register(new AntiqueAtlasClickListener());
        PostBlock.ItemsThatOverrideBlockPartActivation.add(AntiqueAtlasItems.ATLAS);
    }

    public static void registerNetworkPacket() {
        PacketHandler.register(new RequestTeleportByMarkerEvent(), "antiqueatlas".hashCode());
    }

    private static void onWorldLoaded(ServerLevel overworld) {
        WaystoneLibrary.onInitializeDo.addListener(AntiqueAtlasAdapter::onLibraryInitialized);
        storage = overworld.getDataStorage().computeIfAbsent(Storage::load, Storage::new, Storage.name);

        // Hacc to make sure AntiqueAtlas has finished initializing and will not override the following changes.
        Delay.onServerForFrames(100, () -> {
            memorizeOrRemoveUnknownMarkers();
            if(Config.Server.compat.atlas.shouldAddIcons.get())
                registerMissingMarkers();
        });
    }

    private static void onLibraryInitialized(WaystoneLibrary library) {
        library.updateEventDispatcher.addListener(AntiqueAtlasAdapter::onWaystoneUpdated);
    }

    private static void memorizeOrRemoveUnknownMarkers() {
        GlobalMarkersData markerData = AntiqueAtlasMod.globalMarkersData.getData();
        for(ResourceKey<Level> levelKey: markerData.getVisitedDimensions()) {
            DimensionMarkersData dimensionMarkersData = markerData.getMarkersDataInWorld(levelKey);
            Collection<Marker> toRemove = new ArrayList<>();
            for(Marker marker : dimensionMarkersData.getAllMarkers())
                if (marker.getType().equals(markerType)) (
                    Config.Server.compat.atlas.shouldAddIcons.get()
                        ? Optional.of(marker.getLabel().getString())
                        : Optional.<String>empty())
                    .flatMap(name -> WaystoneLibrary.getInstance().getHandleByName(name))
                    .filter(handle -> {
                        BlockPos expectedMarkerPos = WaystoneLibrary.getInstance().getLocationData(handle).block.blockPos;
                        return marker.getX() == expectedMarkerPos.getX() && marker.getZ() == expectedMarkerPos.getZ();
                    }).ifPresentOrElse(
                        handle -> {
                            if(!Objects.equals(storage.registeredMarkers.putIfAbsent(handle, marker.getId()), marker.getId()))
                                storage.setDirty();
                        },
                        () -> toRemove.add(marker)
                    );
            for(Marker marker : toRemove) dimensionMarkersData.removeMarker(marker);
        }
    }

    private static void registerMissingMarkers() {
        for(WaystoneLibrary.WaystoneInfo info: WaystoneLibrary.getInstance().getAllWaystoneInfo()) {
            if(!storage.registeredMarkers.containsKey(info.handle)) {
                info.locationData.block.world
                    .mapRight(rl -> TileEntityUtils.findWorld(rl, false))
                    .rightOr(Optional::of)
                    .ifPresent(level -> addMarker(level, info.name, info.locationData.block.blockPos, info.handle));
            }
        }
    }

    private static void onWaystoneUpdated(WaystoneUpdatedEvent event) {
        event.location.block.world
            .mapRight(rl -> TileEntityUtils.findWorld(rl, false))
            .rightOr(Optional::of)
            .ifPresent(level -> {
                deleteMarkerIfNecessary(level, event);
                addMarkerIfNecessary(level, event);
            });
    }

    private static void deleteMarkerIfNecessary(Level level, WaystoneUpdatedEvent event) {
        if((event.getType() == WaystoneUpdatedEvent.Type.Removed
            || event.getType() == WaystoneUpdatedEvent.Type.Renamed
            || event.getType() == WaystoneUpdatedEvent.Type.Moved
        ) && storage != null && storage.registeredMarkers.containsKey(event.handle)) {
            AtlasAPI.getMarkerAPI().deleteGlobalMarker(level, storage.registeredMarkers.get(event.handle));
            storage.registeredMarkers.remove(event.handle);
            storage.setDirty();
        }
    }

    private static void addMarkerIfNecessary(Level level, WaystoneUpdatedEvent event) {
        if(event.getType() != WaystoneUpdatedEvent.Type.Removed) {
            WorldLocation loc = event instanceof WaystoneMovedEvent moved ? moved.newLocation : event.location.block;
            addMarker(level, event.name, loc.blockPos, event.handle);
        }
    }

    private static void addMarker(Level level, String name, BlockPos pos, WaystoneHandle handle) {
        if(Config.Server.compat.atlas.shouldAddIcons.get()) {
            Marker marker = AtlasAPI.getMarkerAPI().putGlobalMarker(
                level,
                false,
                markerType,
                new TextComponent(name),
                pos.getX(),
                pos.getZ()
            );
            if(marker != null) {
                storage.registeredMarkers.put(handle, marker.getId());
                storage.setDirty();
            }
        }
    }

    private static final class Storage extends SavedData {

        public final HashMap<WaystoneHandle, Integer> registeredMarkers = new HashMap<>();

        public static final String name = Signpost.MOD_ID + "_AntiqueAtlas";

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag listTag = new ListTag();
            for (Map.Entry<WaystoneHandle, Integer> entry: registeredMarkers.entrySet())
                listTag.add(serializeEntry(entry));
            tag.put("registeredMarkers", listTag);
            return tag;
        }

        private CompoundTag serializeEntry(Map.Entry<WaystoneHandle, Integer> entry) {
            CompoundTag entryTag = new CompoundTag();
            CompoundTag waytoneTag = new CompoundTag();
            entry.getKey().write(waytoneTag);
            entryTag.put("waystone", waytoneTag);
            entryTag.putInt("markerId", entry.getValue());
            return entryTag;
        }

        public static Storage load(CompoundTag tag) {
            Storage ret = new Storage();
            Tag untypedListTag = tag.get("registeredMarkers");
            if(untypedListTag instanceof ListTag listTag)
                for(Tag untypedEntryTag: listTag)
                    if (untypedEntryTag instanceof CompoundTag entryTag
                        && entryTag.get("waystone") instanceof CompoundTag waystoneTag
                        && entryTag.get("markerId") instanceof IntTag markerTag
                    ) WaystoneHandle.read(waystoneTag).ifPresent(waystoneHandle ->
                        ret.registeredMarkers.put(waystoneHandle, markerTag.getAsInt())
                    );
            return ret;
        }

    }

}
