package gollorum.signpost.data.blocks;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.gui.utils.TextureResource;
import gollorum.signpost.registry.BlockRegistry;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.loaders.ObjModelBuilder;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;

public class WaystoneModel {

    public static void register(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        var waystoneTexture = TextureResource.waystoneTextureLocation.location();
        blockModels.createTrivialBlock(
            BlockRegistry.WaystoneBlock.get(),
            TexturedModel.createDefault(
                block -> new TextureMapping().put(TextureSlot.ALL, waystoneTexture),
                ModelTemplates.CUBE_ALL
            ));

        for (var variant : ModelWaystone.variants) {
            var textureSlot = TextureSlot.create(waystoneTexture.toString());
            blockModels.createTrivialBlock(
                variant.getBlock(),
                TexturedModel.createDefault(
                    block -> new TextureMapping()
                        .put(textureSlot, waystoneTexture)
                        .put(TextureSlot.PARTICLE, waystoneTexture),
                    ExtendedModelTemplateBuilder.builder()
                        .customLoader(ObjModelBuilder::new, loader -> {
                            loader.modelLocation(ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "models/block/" + variant.registryName + ".obj"));
//                            loader.overrideMaterialLibrary(ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "models/block/waystone_model.mtl"));
                            loader.flipV(true);
                            loader.emissiveAmbient(false);
                            loader.shadeQuads(true);
                        })
                        .requiredTextureSlot(textureSlot)
                        .requiredTextureSlot(TextureSlot.PARTICLE)
                        .parent(ResourceLocation.withDefaultNamespace("block/block"))
                        .build()
                )
            );
        }
    }

}
