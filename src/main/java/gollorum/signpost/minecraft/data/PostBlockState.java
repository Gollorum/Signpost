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
        for (Map.Entry<Post.Variant, BlockModelBuilder> entry : postModel.allModels.entrySet()) {
            getVariantBuilder(entry.getKey().block)
                .partialState().setModels(new ConfiguredModel(entry.getValue()));
        }
        getVariantBuilder(Waystone.INSTANCE)
            .partialState().setModels(new ConfiguredModel(waystoneModel.waystoneModel));

        VariantBlockStateBuilder villageWaystoneBuilder = getVariantBuilder(VillageWaystone.INSTANCE);
        villageWaystoneBuilder.forAllStates(state -> villageWaystoneBuilder.partialState()
            .modelForState()
            .modelFile(waystoneModel.villageWaystoneModel)
            .build());

        VariantBlockStateBuilder villagePostBuilder = getVariantBuilder(VillagePost.INSTANCE);
        villagePostBuilder.forAllStates(state -> villagePostBuilder.partialState()
            .modelForState()
            .modelFile(waystoneModel.villagePostModel)
            .build());

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
