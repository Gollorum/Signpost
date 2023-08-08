package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class BlockModels extends BlockModelProvider {

    public final PostModel postModelProvider;
    public final WaystoneModel waystoneModelProvider;
    public final GeneratorModel generatorModelProvider;

    public BlockModels(DataGenerator generator, PackOutput output, ExistingFileHelper fileHelper) {
        super(output, Signpost.MOD_ID, fileHelper);
        postModelProvider = new PostModel(this);
        waystoneModelProvider = new WaystoneModel(this);
        generatorModelProvider = new GeneratorModel(this);
    }

    @Override
    protected void registerModels() {
        postModelProvider.registerModels();
        waystoneModelProvider.registerModels();
        generatorModelProvider.registerModels();
    }
}
