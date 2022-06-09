package gollorum.signpost.minecraft.data;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

public final class DataGeneration {

    private DataGeneration() {}

    public static void register(IEventBus modBus){
        modBus.register(new DataGeneration());
    }

    @SubscribeEvent
    public void gatherData(GatherDataEvent event) {
        DataGenerator datagenerator = event.getGenerator();
        ExistingFileHelper fileHelper = event.getExistingFileHelper();

        if (event.includeServer()) {

            PostTag.Blocks postBlocksTagProvider = new PostTag.Blocks(datagenerator, fileHelper);
            datagenerator.addProvider(true, postBlocksTagProvider);
            datagenerator.addProvider(true, new PostTag(datagenerator, postBlocksTagProvider, fileHelper));

            WaystoneTag.Blocks waystoneBlocksTagProvider = new WaystoneTag.Blocks(datagenerator, fileHelper);
            datagenerator.addProvider(true, waystoneBlocksTagProvider);
            datagenerator.addProvider(true, new WaystoneTag(datagenerator, waystoneBlocksTagProvider, fileHelper));

            datagenerator.addProvider(true, new PostRecipe(datagenerator));
            datagenerator.addProvider(true, new WaystoneRecipe(datagenerator));
            datagenerator.addProvider(true, new WrenchRecipe(datagenerator));
            datagenerator.addProvider(true, new BrushRecipe(datagenerator));

            datagenerator.addProvider(true, new LootTables(datagenerator));
        }
        if(event.includeClient()) {
            PostModel postModel = new PostModel(datagenerator, fileHelper);
            datagenerator.addProvider(true, postModel);
            WaystoneModel waystoneModel = WaystoneModel.addTo(datagenerator, fileHelper);
            datagenerator.addProvider(true, new WrenchModel(datagenerator, fileHelper));
            datagenerator.addProvider(true, new BrushModel(datagenerator, fileHelper));
            datagenerator.addProvider(true, new PostBlockState(datagenerator, fileHelper, postModel, waystoneModel));
        }
    }

}
