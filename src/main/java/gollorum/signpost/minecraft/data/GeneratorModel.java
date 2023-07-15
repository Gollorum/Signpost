package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.WaystoneGeneratorBlock;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockModelProvider;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class GeneratorModel extends BlockModelProvider {

    public final BlockModelBuilder waystoneModel;

    public static GeneratorModel addTo(DataGenerator generator, PackOutput output, ExistingFileHelper fileHelper) {
        GeneratorModel self = new GeneratorModel(output, fileHelper);
        generator.addProvider(true, self);
        generator.addProvider(true, self.makeItem(output, fileHelper));
        return self;
    }

    private GeneratorModel(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, Signpost.MOD_ID, fileHelper);
        waystoneModel = new BlockModelBuilder(new ResourceLocation(Signpost.MOD_ID, "block/" + WaystoneGeneratorBlock.REGISTRY_NAME), fileHelper);
    }

    @Override
    protected void registerModels() {
        ResourceLocation waystoneTexture = new ResourceLocation(Signpost.MOD_ID, "block/waystone");
        cubeAll(WaystoneGeneratorBlock.REGISTRY_NAME, waystoneTexture);
    }

    private DataProvider makeItem(PackOutput output, ExistingFileHelper fileHelper) {
        return new GeneratorModel.Item(output, fileHelper);
    }

    private class Item extends ItemModelProvider {

        public Item(PackOutput output, ExistingFileHelper existingFileHelper) {
            super(output, Signpost.MOD_ID, existingFileHelper);
        }

        @Override
        protected void registerModels() {
            getBuilder(WaystoneGeneratorBlock.REGISTRY_NAME).parent(waystoneModel);
        }
    }

}
