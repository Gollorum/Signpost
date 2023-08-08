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

            BlockTags blocksTagProvider = new BlockTags(output, lookupProvider, fileHelper);
            datagenerator.addProvider(true, blocksTagProvider);
            datagenerator.addProvider(true, new ItemTags(output, lookupProvider, blocksTagProvider, fileHelper));
            datagenerator.addProvider(true, new Recipes(output));
            datagenerator.addProvider(true, new LootTables(output));
        }
        if(event.includeClient()) {
            BlockModels blockModelProvider = new BlockModels(datagenerator, output, fileHelper);
            datagenerator.addProvider(true, blockModelProvider);
            datagenerator.addProvider(true, new ItemModels(blockModelProvider, output, fileHelper));
            datagenerator.addProvider(true, new BlockStates(output, fileHelper, blockModelProvider.postModelProvider, blockModelProvider.waystoneModelProvider, blockModelProvider.generatorModelProvider));
        }
    }

}
