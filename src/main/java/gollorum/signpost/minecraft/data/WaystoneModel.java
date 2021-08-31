package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.*;
import net.minecraftforge.client.model.generators.loaders.OBJLoaderBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.HashMap;
import java.util.Map;

public class WaystoneModel extends BlockModelProvider {

	public final BlockModelBuilder waystoneModel;
	public static final ResourceLocation inPostLocation = new ResourceLocation(Signpost.MOD_ID, "block/in_post_waystone");
	public final Map<ModelWaystone.Variant, ModelFile> variantModels = new HashMap<>();

	public static WaystoneModel addTo(DataGenerator generator, ExistingFileHelper fileHelper) {
		WaystoneModel self = new WaystoneModel(generator, fileHelper);
		generator.addProvider(self);
		generator.addProvider(self.makeItem(generator, fileHelper));
		return self;
	}

	private WaystoneModel(DataGenerator generator, ExistingFileHelper fileHelper) {
		super(generator, Signpost.MOD_ID, fileHelper);
		waystoneModel = new BlockModelBuilder(new ResourceLocation(Signpost.MOD_ID, "block/" + WaystoneBlock.REGISTRY_NAME), fileHelper);
	}

	@Override
	protected void registerModels() {
		ResourceLocation waystoneTexture = new ResourceLocation(Signpost.MOD_ID, "block/waystone");
		cubeAll(WaystoneBlock.REGISTRY_NAME, waystoneTexture);

        getBuilder(inPostLocation.toString())
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
			BlockModelBuilder builder = getBuilder(loc.toString())
				.parent(new ModelFile.ExistingModelFile(new ResourceLocation("block/block"), existingFileHelper))
				.texture("particle", waystoneTexture)
				.customLoader(OBJLoaderBuilder::begin)
				.modelLocation(new ResourceLocation(loc.getNamespace(), "models/block/" + variant.registryName + ".obj"))
				.flipV(true)
				.diffuseLighting(true)
				.ambientToFullbright(false)
				.end()
				.transforms()
					.transform(ModelBuilder.Perspective.GUI)
						.rotation(30, 315, 0)
						.translation(0, variant.modelYOffset, 0)
						.scale(0.625f)
					.end()
					.transform(ModelBuilder.Perspective.FIRSTPERSON_RIGHT)
						.rotation(0, 315, 0)
						.translation(0, variant.modelYOffset, 0)
						.scale(0.4f)
					.end()
					.transform(ModelBuilder.Perspective.FIRSTPERSON_LEFT)
						.rotation(0, 315, 0)
						.translation(0, variant.modelYOffset, 0)
						.scale(0.4f)
					.end()
				.end();
			variantModels.put(variant, builder);
		}
	}

	private DataProvider makeItem(DataGenerator generator, ExistingFileHelper fileHelper) {
		return new Item(generator, fileHelper);
	}

	private class Item extends ItemModelProvider {

		public Item(DataGenerator generator, ExistingFileHelper existingFileHelper) {
			super(generator, Signpost.MOD_ID, existingFileHelper);
		}

		@Override
		protected void registerModels() {
			getBuilder(WaystoneBlock.REGISTRY_NAME).parent(waystoneModel);
			for(Map.Entry<ModelWaystone.Variant, ModelFile> variant : variantModels.entrySet()) {
				getBuilder(variant.getKey().registryName).parent(variant.getValue());
			}
		}
	}

}
