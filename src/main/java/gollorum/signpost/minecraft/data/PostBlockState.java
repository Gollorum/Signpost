package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.Post;
import gollorum.signpost.minecraft.block.Waystone;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
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
        for (Map.Entry<Post.Info, BlockModelBuilder> entry : postModel.allModels.entrySet()) {
            getVariantBuilder(entry.getKey().post)
                .partialState().setModels(new ConfiguredModel(entry.getValue()));
        }
        getVariantBuilder(Waystone.INSTANCE)
            .partialState().setModels(new ConfiguredModel(waystoneModel.waystoneModel));
    }

}
