package gollorum.signpost.minecraft.worldgen;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.TextComponents;
import gollorum.signpost.utils.WaystoneData;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Optional;

public class WaystoneDiscoveryEventListener {

    public static void register(IEventBus bus) { bus.register(WaystoneDiscoveryEventListener.class); }

    @SubscribeEvent
    public static void onChunkEntered(EntityEvent.EnteringChunk event) {
        if(!event.isCanceled() && event.getEntity() instanceof ServerPlayerEntity && WaystoneLibrary.hasInstance()) {
            WaystoneHandle.Vanilla handle = WaystoneJigsawPiece.getAllEntriesByChunk().get(
                new WaystoneJigsawPiece.ChunkEntryKey(
                    new ChunkPos(event.getNewChunkX(), event.getNewChunkZ()),
                    event.getEntity().level.dimension().location()
                )
            );
            if(handle != null) {
                WaystoneLibrary.getInstance().getData(handle).ifPresent(data -> {
                    if(WaystoneLibrary.getInstance().addDiscovered(new PlayerHandle(event.getEntity()), handle)) {
                        event.getEntity().sendMessage(
                            new TranslationTextComponent(
                                LangKeys.discovered,
                                TextComponents.waystone((ServerPlayerEntity) event.getEntity(), data.name)
                            ), Util.NIL_UUID);
                    }
                });
            }
        }
    }

}
