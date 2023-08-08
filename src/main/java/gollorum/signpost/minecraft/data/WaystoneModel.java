package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.generators.*;
import net.minecraftforge.client.model.generators.loaders.ObjModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.HashMap;
import java.util.Map;

public class WaystoneModel {

	private final BlockModels blockModelProvider;
	public final BlockModelBuilder waystoneModel;
	public static final ResourceLocation inPostLocation = new ResourceLocation(Signpost.MOD_ID, "block/in_post_waystone");
	public final Map<ModelWaystone.Variant, ModelFile> variantModels = new HashMap<>();

//	public static WaystoneModel addTo(BlockModels blockModelProvider) {
//		WaystoneModel self = new WaystoneModel(packOutput, fileHelper);
////		generator.addProvider(true, self);
////		generator.addProvider(true, self.makeItem(packOutput, fileHelper));
//		return self;
//	}

	public WaystoneModel(BlockModels blockModelProvider) {
		this.blockModelProvider = blockModelProvider;
		waystoneModel = new BlockModelBuilder(new ResourceLocation(Signpost.MOD_ID, "block/" + WaystoneBlock.REGISTRY_NAME), blockModelProvider.existingFileHelper);
	}

	public void registerModels() {
		ResourceLocation waystoneTexture = new ResourceLocation(Signpost.MOD_ID, "block/waystone");
		blockModelProvider.cubeAll(WaystoneBlock.REGISTRY_NAME, waystoneTexture);

		blockModelProvider.getBuilder(inPostLocation.toString())
            .element()
                .from(-3, 0, -3)
                .to(3, 6, 3)
		        .allFaces((dir, builder) -> builder
			        .texture("#texture")
		            .uvs(5, 5, 11, 11))
	        .end()
        .texture("texture", waystoneTexture);

		for(ModelWaystone.Variant variant : ModelWaystone.variants) {
			ResourceLocation loc = new ResourceLocation(Signpost.MOD_ID, "block/" + variant.registryName);
			BlockModelBuilder builder = blockModelProvider.getBuilder(loc.toString())
				.parent(new ModelFile.ExistingModelFile(new ResourceLocation("block/block"), blockModelProvider.existingFileHelper))
				.texture("particle", waystoneTexture)
				.customLoader(ObjModelBuilder::begin)
				.modelLocation(new ResourceLocation(loc.getNamespace(), "models/block/" + variant.registryName + ".obj"))
				.flipV(true)
				.shadeQuads(true)
				.emissiveAmbient(false)
				.end()
				.transforms()
					.transform(ItemDisplayContext.GUI)
						.rotation(30, 315, 0)
						.translation(0, variant.modelYOffset, 0)
						.scale(0.625f)
					.end()
					.transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
						.rotation(0, 315, 0)
						.translation(0, variant.modelYOffset, 0)
						.scale(0.4f)
					.end()
					.transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
						.rotation(0, 315, 0)
						.translation(0, variant.modelYOffset, 0)
						.scale(0.4f)
					.end()
				.end();
			variantModels.put(variant, builder);
		}
	}

}
