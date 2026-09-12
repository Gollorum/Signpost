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

    /**
     * 1.21.1 has one {@code GatherDataEvent} with {@code includeClient} / {@code includeServer} flags
     * rather than the split {@code GatherDataEvent.Client}, no {@code createProvider} helper, and its
     * providers still take an {@link net.neoforged.neoforge.common.data.ExistingFileHelper}.
     */
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        var lookup = event.getLookupProvider();
        var existing = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(), new Models(output, existing));
        generator.addProvider(event.includeClient(), new ItemModels(output, existing));

        var blockTags = new BlockTags(output, lookup, existing);
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(), new ItemTags(blockTags, output, lookup, existing));
        generator.addProvider(event.includeServer(), new Recipes(output, lookup));
        generator.addProvider(event.includeServer(), new LootTables(output, lookup));
        generator.addProvider(event.includeServer(), new DatapackBuiltinEntriesProvider(
            output, lookup,
            new RegistrySetBuilder().add(ModelTypeRegistry.REGISTRY_KEY, PostModelTypes::run),
            Set.of(Signpost.MOD_ID)));
    }

}
