package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.registry.BlockRegistry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.common.data.ExistingFileHelper;

public class GeneratorState extends BlockStateProvider {

    private final GeneratorModel model;

    public GeneratorState(
        PackOutput output,
        ExistingFileHelper fileHelper,
        GeneratorModel model
    ) {
        super(output, Signpost.MOD_ID, fileHelper);
        this.model = model;
    }

    @Override
    protected void registerStatesAndModels() {
        getVariantBuilder(BlockRegistry.WaystoneGenerator.get())
            .partialState().setModels(new ConfiguredModel(model.waystoneModel));
    }
}
