package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.*;
import net.minecraft.data.DataGenerator;
import net.minecraft.util.Direction;
import net.minecraftforge.client.model.generators.*;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.Map;

public class PostBlockState extends BlockStateProvider {

    private final PostModel postModel;
    private final WaystoneModel waystoneModel;

    public PostBlockState(
        DataGenerator gen,
        ExistingFileHelper fileHelper,
        PostModel postModel,
        WaystoneModel waystoneModel
    ) {
        super(gen, Signpost.MOD_ID, fileHelper);
        this.postModel = postModel;
        this.waystoneModel = waystoneModel;
    }

    @Override
    protected void registerStatesAndModels() {
        for (Map.Entry<PostBlock.Variant, BlockModelBuilder> entry : postModel.allModels.entrySet()) {
            getVariantBuilder(entry.getKey().block)
                .partialState().setModels(new ConfiguredModel(entry.getValue()));
        }
        getVariantBuilder(WaystoneBlock.INSTANCE)
            .partialState().setModels(new ConfiguredModel(waystoneModel.waystoneModel));

        for(Map.Entry<ModelWaystone.Variant, ModelFile> entry : waystoneModel.variantModels.entrySet()) {
            VariantBlockStateBuilder builder = getVariantBuilder(entry.getKey().block);
            builder.forAllStatesExcept(
                state -> builder
                    .partialState()
                    .modelForState()
                    .modelFile(entry.getValue())
                    .rotationY((((Direction) state.getValues().get(ModelWaystone.Facing)).getHorizontalIndex() - 1) * 90)
                    .build(),
                ModelWaystone.Waterlogged
            );
        }
    }

}
