package gollorum.signpost.minecraft.data;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

public final class DataGeneration {

    public static void register(IEventBus modBus){
        modBus.register(new DataGeneration());
    }

    private DataGeneration() {}

    @SubscribeEvent
    public void gatherData(GatherDataEvent event) {
        DataGenerator datagenerator = event.getGenerator();
        ExistingFileHelper fileHelper = event.getExistingFileHelper();

        if (event.includeServer()) {
            PostTag.Blocks blocksTagProvider = new PostTag.Blocks(datagenerator, fileHelper);
            datagenerator.addProvider(blocksTagProvider);
            datagenerator.addProvider(new PostTag(datagenerator, blocksTagProvider, fileHelper));
            datagenerator.addProvider(new PostRecipe(datagenerator));
            datagenerator.addProvider(new WaystoneRecipe(datagenerator));
            datagenerator.addProvider(new WrenchRecipe(datagenerator));
        }
        if(event.includeClient()) {
            PostModel postModel = new PostModel(datagenerator, fileHelper);
            datagenerator.addProvider(postModel);
            datagenerator.addProvider(new WrenchModel(datagenerator, fileHelper));
            datagenerator.addProvider(new PostBlockState(datagenerator, fileHelper, postModel));
        }
    }

}
