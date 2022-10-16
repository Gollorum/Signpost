package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.items.GenerationWand;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

public class GeneratorWandModel extends ItemModelProvider {

    public GeneratorWandModel(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, Signpost.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        getBuilder(GenerationWand.registryName).parent(
            new ModelFile.ExistingModelFile(new ResourceLocation("item/handheld"), existingFileHelper)
        ).texture("layer0", new ResourceLocation(Signpost.MOD_ID, "item/generation_wand"));
    }

}
