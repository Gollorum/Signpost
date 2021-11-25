package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.items.Wrench;
import net.minecraft.data.DataGenerator;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.ExistingFileHelper;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;

public class WrenchModel extends ItemModelProvider {

    public WrenchModel(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, Signpost.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        getBuilder(Wrench.registryName).parent(
            new ModelFile.ExistingModelFile(new ResourceLocation("item/handheld"),
                existingFileHelper))
        .texture("layer0", new ResourceLocation(Signpost.MOD_ID, "item/tool"));
    }

    @Override
    public String getName() {
        return "signpost wrench";
    }
}
