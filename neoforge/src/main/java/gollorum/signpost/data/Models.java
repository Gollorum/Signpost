package gollorum.signpost.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.data.blocks.GeneratorModel;
import gollorum.signpost.data.blocks.PostModel;
import gollorum.signpost.data.blocks.WaystoneModel;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;

public class Models extends ModelProvider {

    public Models(PackOutput output) {
        super(new PackOutputWithObjModelSpecialHandling(output), Signpost.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        PostModel.register(blockModels, itemModels);
        GeneratorModel.register(blockModels, itemModels);
        WaystoneModel.register(blockModels, itemModels);
        Items.register(itemModels);
    }

    private static class PackOutputWithObjModelSpecialHandling extends PackOutput {
        private final PackOutput original;

        private PackOutputWithObjModelSpecialHandling(PackOutput original) {
            super(original.getOutputFolder());
            this.original = original;
        }

        @Override
        public PackOutput.PathProvider createPathProvider(Target target, String kind) {
            return kind.equals("models")
                ? new PathProvider(this, target, kind)
                : super.createPathProvider(target, kind);
        }

        public static class PathProvider extends PackOutput.PathProvider {

            private final Path neoForgeRoot;

            public PathProvider(PackOutput output, Target target, String kind) {
                super(output, target, kind);
                // Currently: project/common/src/generated/resources/assets
                this.neoForgeRoot = output.getOutputFolder(target)
                    .getParent() // resources
                    .getParent() // generated
                    .getParent() // src
                    .getParent() // common
                    .getParent() // project
                    .resolve("neoforge\\src\\generated\\resources\\assets");
            }

            @Override
            public Path json(ResourceLocation location) {
                if (location.getNamespace().equals(Signpost.MOD_ID)
                    && location.getPath().startsWith("block/waystone_model")
                ) {
                    return neoForgeRoot.resolve(location.getNamespace())
                        .resolve("models")
                        .resolve(location.getPath() + ".json");
                }
                return super.json(location);
            }
        }
    }
}
