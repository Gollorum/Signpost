package gollorum.signpost.registry;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.TextComponents;
import gollorum.signpost.minecraft.worldgen.IWaystoneDiscoveryEventListener;
import gollorum.signpost.minecraft.worldgen.VillageWaystone;
import gollorum.signpost.utils.WaystoneData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.Optional;

public class WaystoneDiscoveryEventListener implements IWaystoneDiscoveryEventListener {

    private static final int discoveryDistance = 8;

    public static void register() {
        ServerTickEvents.START_WORLD_TICK.register(WaystoneDiscoveryEventListener::onTick);
    }

    private static void onTick(ServerLevel level) {
        var allEntries = VillageWaystone.getAllEntriesByChunk(false);
        for(var player : level.players()) {
            var chunkRadius = 1 + (discoveryDistance >> 4);
            var playerChunk = player.chunkPosition();
            for(var x = -chunkRadius; x <= chunkRadius; x++) {
                for(var z = -chunkRadius; z <= chunkRadius; z++) {
                    var key = new VillageWaystone.ChunkEntryKey(
                        new ChunkPos(playerChunk.x + x, playerChunk.z + z),
                        level.dimension().location()
                    );
                    var handle = allEntries.get(key);
                    if(handle != null && !WaystoneLibrary.getInstance().isDiscovered(new PlayerHandle(player), handle)) {
                        Optional<WaystoneData> dataOption = WaystoneLibrary.getInstance().getData(handle);
                        dataOption.ifPresent(data -> {
                            if(WaystoneLibrary.getInstance().addDiscovered(new PlayerHandle(player), handle)) {
                                player.sendSystemMessage(
                                    Component.translatable(
                                        LangKeys.discovered,
                                        TextComponents.waystone(player, data.name)
                                    ));
                            }
                        });
                    }
                }
            }
        }
    }

    @Override
    public void initialize() { }

    @Override
    public void registerNew(WaystoneHandle.Vanilla handle, ServerLevel world, BlockPos pos) { }
}
