package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.WaystoneGeneratorBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockModelBuilder;

public class GeneratorModel {

    public final BlockModelBuilder generatorModel;
    private final BlockModels blockModelProvider;

    public GeneratorModel(BlockModels blockModelProvider) {
        this.blockModelProvider = blockModelProvider;
        generatorModel = new BlockModelBuilder(new ResourceLocation(Signpost.MOD_ID, "block/" + WaystoneGeneratorBlock.REGISTRY_NAME), blockModelProvider.existingFileHelper);
    }

    public void registerModels() {
        ResourceLocation waystoneTexture = new ResourceLocation(Signpost.MOD_ID, "block/waystone");
        blockModelProvider.cubeAll(WaystoneGeneratorBlock.REGISTRY_NAME, waystoneTexture);
    }
}
