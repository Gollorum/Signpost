package gollorum.signpost.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.data.blocks.GeneratorModel;
import gollorum.signpost.data.blocks.PostModel;
import gollorum.signpost.data.blocks.WaystoneModel;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;

public class Models extends ModelProvider {

    public Models(PackOutput output) {
        super(output, Signpost.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        PostModel.register(blockModels, itemModels);
        GeneratorModel.register(blockModels, itemModels);
        WaystoneModel.register(blockModels, itemModels);
        Items.register(itemModels);
    }

}
