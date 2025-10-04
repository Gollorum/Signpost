package gollorum.signpost.data;

import gollorum.signpost.Signpost;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = Signpost.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class DataGeneration {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        event.createProvider(Models::new);
        var blockTags = event.createProvider(BlockTags::new);
        event.createProvider(Recipes.Runner::new);
        event.createProvider(LootTables::new);
        event.createProvider((packOut, look) -> new ItemTags(blockTags, packOut, look));
    }

}