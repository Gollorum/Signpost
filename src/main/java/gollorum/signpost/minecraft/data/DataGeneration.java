package gollorum.signpost.minecraft.data;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.ExistingFileHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

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

            PostTag.Blocks postBlocksTagProvider = new PostTag.Blocks(datagenerator);
            datagenerator.addProvider(postBlocksTagProvider);
            datagenerator.addProvider(new PostTag(datagenerator));

            WaystoneTag.Blocks waystoneBlocksTagProvider = new WaystoneTag.Blocks(datagenerator);
            datagenerator.addProvider(waystoneBlocksTagProvider);
            datagenerator.addProvider(new WaystoneTag(datagenerator));

            datagenerator.addProvider(new PostRecipe(datagenerator));
            datagenerator.addProvider(new WaystoneRecipe(datagenerator));
            datagenerator.addProvider(new WrenchRecipe(datagenerator));
            datagenerator.addProvider(new BrushRecipe(datagenerator));

            datagenerator.addProvider(new LootTables(datagenerator));
        }
        if(event.includeClient()) {
            PostModel postModel = new PostModel(datagenerator, fileHelper);
            datagenerator.addProvider(postModel);
            WaystoneModel waystoneModel = WaystoneModel.addTo(datagenerator, fileHelper);
            datagenerator.addProvider(new WrenchModel(datagenerator, fileHelper));
            datagenerator.addProvider(new BrushModel(datagenerator, fileHelper));
            datagenerator.addProvider(new PostBlockState(datagenerator, fileHelper, postModel, waystoneModel));
        }
    }

}
