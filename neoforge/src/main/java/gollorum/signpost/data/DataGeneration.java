package gollorum.signpost.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.data.ModelTypeRegistry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Set;

@EventBusSubscriber(modid = Signpost.MOD_ID)
public final class DataGeneration {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        // World layer first: the reloadable layer below is built on top of whatever this registers.
        event.createWorldRegistryObjects(
            new RegistrySetBuilder()
                .add(ModelTypeRegistry.REGISTRY_KEY, PostModelTypes::run),
            Set.of(Signpost.MOD_ID));
        // Loot tables, recipes and advancements are reloadable datapack registries since 26.3.
        event.createReloadableRegistryObjects(
            new RegistrySetBuilder()
                .add(Registries.LOOT_TABLE, LootTables.create())
                .add(Recipes.create()),
            Set.of(Signpost.MOD_ID));
        event.createProvider(Models::new);
        var blockTags = event.createProvider(BlockTags::new);
        event.createProvider((packOut, look) -> new ItemTags(blockTags, packOut, look));
//        event.createProvider(PostModelTypes::new);
    }

}