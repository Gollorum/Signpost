package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.registry.BlockRegistry;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.common.data.ExistingFileHelper;

public class GeneratorState extends BlockStateProvider {

    private final GeneratorModel model;

    public GeneratorState(
        DataGenerator gen,
        ExistingFileHelper fileHelper,
        GeneratorModel model
    ) {
        super(gen, Signpost.MOD_ID, fileHelper);
        this.model = model;
    }

    @Override
    protected void registerStatesAndModels() {
        getVariantBuilder(BlockRegistry.WaystoneGenerator.get())
            .partialState().setModels(new ConfiguredModel(model.waystoneModel));
    }
}
