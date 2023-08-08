package gollorum.signpost.minecraft.data;

import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import net.minecraft.core.Direction;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.VariantBlockStateBuilder;

import java.util.Map;

public class PostBlockState {

    private final PostModel postModel;
    private final WaystoneModel waystoneModel;
    private final BlockStates blockStatesProvider;

    public PostBlockState(
        PostModel postModel,
        WaystoneModel waystoneModel,
        BlockStates blockStatesProvider
    ) {
        this.postModel = postModel;
        this.waystoneModel = waystoneModel;
        this.blockStatesProvider = blockStatesProvider;
    }

    public void registerStatesAndModels() {
        for (Map.Entry<PostBlock.Variant, BlockModelBuilder> entry : postModel.allModels.entrySet()) {
            blockStatesProvider.getVariantBuilder(entry.getKey().getBlock())
                .partialState().setModels(new ConfiguredModel(entry.getValue()));
        }
        blockStatesProvider.getVariantBuilder(WaystoneBlock.getInstance())
            .partialState().setModels(new ConfiguredModel(waystoneModel.waystoneModel));

        for(Map.Entry<ModelWaystone.Variant, ModelFile> entry : waystoneModel.variantModels.entrySet()) {
            VariantBlockStateBuilder builder = blockStatesProvider.getVariantBuilder(entry.getKey().getBlock());
            builder.forAllStatesExcept(
                state -> builder
                    .partialState()
                    .modelForState()
                    .modelFile(entry.getValue())
                    .rotationY((((Direction) state.getValues().get(ModelWaystone.Facing)).get2DDataValue() - 1) * 90)
                    .build(),
                ModelWaystone.Waterlogged
            );
        }
    }

}
