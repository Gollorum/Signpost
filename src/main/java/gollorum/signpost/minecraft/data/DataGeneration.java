package gollorum.signpost.minecraft.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.concurrent.CompletableFuture;

public final class DataGeneration {

    private DataGeneration() {}

    public static void register(IEventBus modBus){
        modBus.register(new DataGeneration());
    }

    @SubscribeEvent
    public void gatherData(GatherDataEvent event) {
        DataGenerator datagenerator = event.getGenerator();
        ExistingFileHelper fileHelper = event.getExistingFileHelper();
        PackOutput output = event.getGenerator().getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        if (event.includeServer()) {

            PostTag.Blocks postBlocksTagProvider = new PostTag.Blocks(output, lookupProvider, fileHelper);
            datagenerator.addProvider(true, postBlocksTagProvider);
            datagenerator.addProvider(true, new PostTag(output, lookupProvider, postBlocksTagProvider, fileHelper));

            WaystoneTag.Blocks waystoneBlocksTagProvider = new WaystoneTag.Blocks(output, lookupProvider, fileHelper);
            datagenerator.addProvider(true, waystoneBlocksTagProvider);
            datagenerator.addProvider(true, new WaystoneTag(output, lookupProvider, waystoneBlocksTagProvider, fileHelper));

            datagenerator.addProvider(true, new PostRecipe(output));
            datagenerator.addProvider(true, new WaystoneRecipe(output));
            datagenerator.addProvider(true, new WrenchRecipe(output));
            datagenerator.addProvider(true, new BrushRecipe(output));

            datagenerator.addProvider(true, new LootTables(output));
        }
        if(event.includeClient()) {
            PostModel postModel = new PostModel(datagenerator, output, fileHelper);
            datagenerator.addProvider(true, postModel);
            WaystoneModel waystoneModel = WaystoneModel.addTo(datagenerator, output, fileHelper);
            GeneratorModel generatorModel = GeneratorModel.addTo(datagenerator, output, fileHelper);
            datagenerator.addProvider(true, new WrenchModel(output, fileHelper));
            datagenerator.addProvider(true, new BrushModel(output, fileHelper));
            datagenerator.addProvider(true, new GeneratorWandModel(output, fileHelper));
            datagenerator.addProvider(true, new PostBlockState(output, fileHelper, postModel, waystoneModel));
            datagenerator.addProvider(true, new GeneratorState(output, fileHelper, generatorModel));
        }
    }

}
