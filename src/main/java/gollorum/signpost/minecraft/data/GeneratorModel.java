package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.WaystoneGeneratorBlock;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockModelProvider;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class GeneratorModel extends BlockModelProvider {

    public final BlockModelBuilder waystoneModel;

    public static GeneratorModel addTo(DataGenerator generator, ExistingFileHelper fileHelper) {
        GeneratorModel self = new GeneratorModel(generator, fileHelper);
        generator.addProvider(self);
        generator.addProvider(self.makeItem(generator, fileHelper));
        return self;
    }

    private GeneratorModel(DataGenerator generator, ExistingFileHelper fileHelper) {
        super(generator, Signpost.MOD_ID, fileHelper);
        waystoneModel = new BlockModelBuilder(new ResourceLocation(Signpost.MOD_ID, "block/" + WaystoneGeneratorBlock.REGISTRY_NAME), fileHelper);
    }

    @Override
    protected void registerModels() {
        ResourceLocation waystoneTexture = new ResourceLocation(Signpost.MOD_ID, "block/waystone");
        cubeAll(WaystoneGeneratorBlock.REGISTRY_NAME, waystoneTexture);
    }

    private DataProvider makeItem(DataGenerator generator, ExistingFileHelper fileHelper) {
        return new GeneratorModel.Item(generator, fileHelper);
    }

    private class Item extends ItemModelProvider {

        public Item(DataGenerator generator, ExistingFileHelper existingFileHelper) {
            super(generator, Signpost.MOD_ID, existingFileHelper);
        }

        @Override
        protected void registerModels() {
            getBuilder(WaystoneGeneratorBlock.REGISTRY_NAME).parent(waystoneModel);
        }
    }

}
