package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.registry.BlockRegistry;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.common.data.ExistingFileHelper;

public class BlockStates extends BlockStateProvider {

    private final GeneratorModel model;

    private final PostBlockState postBlockStateProvider;

    public BlockStates(
        PackOutput output,
        ExistingFileHelper fileHelper,
        PostModel postModel,
        WaystoneModel waystoneModel,
        GeneratorModel model
    ) {
        super(output, Signpost.MOD_ID, fileHelper);
        this.model = model;
        this.postBlockStateProvider = new PostBlockState(postModel, waystoneModel, this);
    }

    @Override
    protected void registerStatesAndModels() {
        getVariantBuilder(BlockRegistry.WaystoneGenerator.get())
            .partialState().setModels(new ConfiguredModel(model.generatorModel));
        postBlockStateProvider.registerStatesAndModels();
    }
}
