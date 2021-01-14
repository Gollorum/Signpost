package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.Post;
import gollorum.signpost.minecraft.block.Waystone;
import net.minecraft.data.DataGenerator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Direction;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockModelProvider;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class WaystoneModel extends BlockModelProvider {

	public final BlockModelBuilder waystoneModel;
	public static final ResourceLocation inPostLocation = new ResourceLocation(Signpost.MOD_ID, "block/in_post_waystone");

	public WaystoneModel(DataGenerator generator, ExistingFileHelper fileHelper) {
		super(generator, Signpost.MOD_ID, fileHelper);
		waystoneModel = new BlockModelBuilder(new ResourceLocation(Signpost.MOD_ID, "block/" + Waystone.REGISTRY_NAME), fileHelper);
		generator.addProvider(new WaystoneModel.Item(generator, fileHelper));
	}

	@Override
	protected void registerModels() {
		ResourceLocation waystoneTexture = new ResourceLocation(Signpost.MOD_ID, "block/waystone");
		cubeAll(Waystone.REGISTRY_NAME, waystoneTexture);

        getBuilder(inPostLocation.toString())
            .element()
                .from(-3, 0, -3)
                .to(3, 6, 3)
		        .allFaces((dir, builder) -> builder
			        .texture("#texture")
		            .uvs(5, 5, 11, 11))
	        .end()
        .texture("texture", waystoneTexture);
	}

	private class Item extends ItemModelProvider {

		public Item(DataGenerator generator, ExistingFileHelper existingFileHelper) {
			super(generator, Signpost.MOD_ID, existingFileHelper);
		}

		@Override
		protected void registerModels() {
			getBuilder(Waystone.REGISTRY_NAME).parent(waystoneModel);
		}
	}

}
