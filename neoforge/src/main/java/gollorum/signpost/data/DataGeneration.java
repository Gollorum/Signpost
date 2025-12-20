package gollorum.signpost.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.data.ModelTypeRegistry;
import net.minecraft.core.RegistrySetBuilder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Set;

@EventBusSubscriber(modid = Signpost.MOD_ID)
public final class DataGeneration {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        event.getGenerator().addProvider(true,
            new DatapackBuiltinEntriesProvider(
                event.getGenerator().getPackOutput(), event.getLookupProvider(),
                new RegistrySetBuilder()
                    .add(ModelTypeRegistry.REGISTRY_KEY, PostModelTypes::run),
                Set.of(Signpost.MOD_ID)));
        event.createProvider(Models::new);
        var blockTags = event.createProvider(BlockTags::new);
        event.createProvider(Recipes.Runner::new);
        event.createProvider(LootTables::new);
        event.createProvider((packOut, look) -> new ItemTags(blockTags, packOut, look));
//        event.createProvider(PostModelTypes::new);
    }

}