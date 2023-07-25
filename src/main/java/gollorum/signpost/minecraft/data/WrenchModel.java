package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.items.Wrench;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

public class WrenchModel extends ItemModelProvider {

    public WrenchModel(PackOutput packOutput, ExistingFileHelper existingFileHelper) {
        super(packOutput, Signpost.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        getBuilder(Wrench.registryName).parent(
            new ModelFile.ExistingModelFile(new ResourceLocation("item/handheld"),
                existingFileHelper))
        .texture("layer0", new ResourceLocation(Signpost.MOD_ID, "item/tool"));
    }

}
