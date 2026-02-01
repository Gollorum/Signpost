package gollorum.signpost.data.blocks;

import gollorum.signpost.minecraft.gui.utils.TextureResource;
import gollorum.signpost.registry.BlockRegistry;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;

public class GeneratorModel {

    public static void register(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        blockModels.createTrivialBlock(
            BlockRegistry.WaystoneGenerator.get(),

            TexturedModel.createDefault(
                block -> new TextureMapping().put(TextureSlot.ALL, TextureResource.waystoneTextureLocation.identifier()),
                ModelTemplates.CUBE_ALL
            )
        );
    }
    
}
