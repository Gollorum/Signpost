package gollorum.signpost.minecraft.worldgen;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.utils.LangKeys;
import io.netty.util.internal.PlatformDependent;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
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
        WaystoneHandle.Vanilla handle = WaystoneJigsawPiece.generatedWaystonesByChunk.get(
            new WaystoneJigsawPiece.ChunkEntryKey(
                event.getPos(),
                event.getPlayer().level.dimension().location()
            )
        );
        if(handle != null && !WaystoneLibrary.getInstance().isDiscovered(PlayerHandle.from(event.getPlayer()), handle)) {
            trackedPlayers.computeIfAbsent(event.getPlayer(), p -> PlatformDependent.newConcurrentHashMap())
                .put(handle, WaystoneLibrary.getInstance().getData(handle).location.block.blockPos);
        }
    }

    @SubscribeEvent
    public static void onUnWatchChunk(ChunkWatchEvent.UnWatch event) {
        ConcurrentMap<WaystoneHandle.Vanilla, BlockPos> set = trackedPlayers.get(event.getPlayer());
        if(set == null) return;
        WaystoneHandle.Vanilla handle = WaystoneJigsawPiece.generatedWaystonesByChunk.get(
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
                    if(WaystoneLibrary.getInstance().addDiscovered(new PlayerHandle(map.getKey()), inner.getKey())) {
                        map.getKey().sendMessage(
                            new TranslatableComponent(
                                LangKeys.discovered,
                                Colors.wrap(WaystoneLibrary.getInstance().getData(inner.getKey()).name, Colors.highlight)
                            ), Util.NIL_UUID);
                    }
                    map.getValue().remove(inner.getKey());
                }
            }
            if(map.getValue().isEmpty()) trackedPlayers.remove(map.getKey());
        }
    }

}
