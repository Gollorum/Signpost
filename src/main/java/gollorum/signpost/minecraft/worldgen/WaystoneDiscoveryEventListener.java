package gollorum.signpost.minecraft.worldgen;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.TextComponents;
import gollorum.signpost.utils.WaystoneData;
import io.netty.util.internal.PlatformDependent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

public class WaystoneDiscoveryEventListener {

    private static final int discoveryDistance = 8;

    public static void register(IEventBus bus) { bus.register(WaystoneDiscoveryEventListener.class); }

    private static ConcurrentMap<ServerPlayer, ConcurrentMap<WaystoneHandle.Vanilla, BlockPos>> trackedPlayers;
    public static void initialize() {
        trackedPlayers = PlatformDependent.newConcurrentHashMap();
    }

    @SubscribeEvent
    public static void onWatchChunk(ChunkWatchEvent.Watch event) {
        if(!WaystoneLibrary.hasInstance()) return;
        WaystoneJigsawPiece.ChunkEntryKey key = new WaystoneJigsawPiece.ChunkEntryKey(
            event.getPos(),
            event.getPlayer().level.dimension().location()
        );
        Map<WaystoneJigsawPiece.ChunkEntryKey, WaystoneHandle.Vanilla> allEntries = WaystoneJigsawPiece.getAllEntriesByChunk();
        WaystoneHandle.Vanilla handle = allEntries.get(key);
        if(handle != null && !WaystoneLibrary.getInstance().isDiscovered(PlayerHandle.from(event.getPlayer()), handle)) {
            Optional<WaystoneData> dataOption = WaystoneLibrary.getInstance().getData(handle);
            dataOption.ifPresentOrElse(
                data -> trackedPlayers.computeIfAbsent(event.getPlayer(), p -> PlatformDependent.newConcurrentHashMap())
                    .putIfAbsent(handle, data.location.block.blockPos),
                () -> allEntries.remove(key)
            );
        }
    }

    @SubscribeEvent
    public static void onUnWatchChunk(ChunkWatchEvent.UnWatch event) {
        ConcurrentMap<WaystoneHandle.Vanilla, BlockPos> set = trackedPlayers.get(event.getPlayer());
        if(set == null) return;
        WaystoneHandle.Vanilla handle = WaystoneJigsawPiece.getAllEntriesByChunk().get(
            new WaystoneJigsawPiece.ChunkEntryKey(
                event.getPos(),
                event.getPlayer().level.dimension().location()
            )
        );
        if(handle == null) return;
        set.remove(handle);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if(!WaystoneLibrary.hasInstance()) return;
        for(Map.Entry<ServerPlayer, ConcurrentMap<WaystoneHandle.Vanilla, BlockPos>> map : trackedPlayers.entrySet()) {
            for(Map.Entry<WaystoneHandle.Vanilla, BlockPos> inner : map.getValue().entrySet()) {
                if(inner.getValue().closerThan(map.getKey().blockPosition(), discoveryDistance)) {
                    WaystoneLibrary.getInstance().getData(inner.getKey()).ifPresent(data -> {
                        if(WaystoneLibrary.getInstance().addDiscovered(new PlayerHandle(map.getKey()), inner.getKey())) {
                            map.getKey().sendSystemMessage(
                                Component.translatable(
                                    LangKeys.discovered,
                                    TextComponents.waystone(map.getKey(), data.name)
                                ));
                        }
                    });
                    map.getValue().remove(inner.getKey());
                }
            }
            if(map.getValue().isEmpty()) trackedPlayers.remove(map.getKey());
        }
    }

    public static void registerNew(WaystoneHandle.Vanilla handle, ServerLevel world, BlockPos pos) {
        Signpost.getServerInstance().getPlayerList().getPlayers().forEach(
            player -> {
                if(player.getLevel().equals(world) && player.blockPosition().closerThan(pos, 100))
                    trackedPlayers.computeIfAbsent(player, p -> PlatformDependent.newConcurrentHashMap())
                        .putIfAbsent(handle, pos);
            }
        );
    }
}
