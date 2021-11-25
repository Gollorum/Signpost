package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.IDataProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.*;

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
			variantModels.put(variant, new ModelFile.ExistingModelFile(loc, existingFileHelper));
		}
	}

	private IDataProvider makeItem(DataGenerator generator, ExistingFileHelper fileHelper) {
		return new Item(generator, fileHelper);
	}

	@Override
	public String getName() {
		return "waystone model block";
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

		@Override
		public String getName() {
			return "waystone model item";
		}
	}

}
