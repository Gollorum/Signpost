package gollorum.signpost.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.block.WaystoneGeneratorBlock;
import gollorum.signpost.minecraft.items.Brush;
import gollorum.signpost.minecraft.items.GenerationWand;
import gollorum.signpost.minecraft.items.Wrench;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * 1.21.1 has no {@code assets/<namespace>/items/} model definitions and no {@code special} item model:
 * an item's model is a plain model at {@code models/item/<name>.json}. The post items parent
 * {@code builtin/entity} so that {@link gollorum.signpost.minecraft.rendering.PostItemRenderer} draws
 * them, and carry the display transforms they would otherwise inherit from a block model.
 */
public class ItemModels extends ItemModelProvider {

    public ItemModels(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Signpost.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        handheld(Brush.registryName);
        handheld(Wrench.registryName);
        handheld(GenerationWand.registryName);

        PostBlock.all().forEach(block -> builtinEntity(block.registryName));

        parentBlockModel(WaystoneBlock.REGISTRY_NAME);
        parentBlockModel(WaystoneGeneratorBlock.REGISTRY_NAME);
        for (var variant : ModelWaystone.variants)
            parentBlockModel(variant.registryName);
    }

    private void handheld(String name) {
        withExistingParent(name, ResourceLocation.withDefaultNamespace("item/handheld"))
            .texture("layer0", ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "item/" + name));
    }

    private void parentBlockModel(String name) {
        withExistingParent(name, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "block/" + name));
    }

    /**
     * {@code builtin/entity} inherits no display transforms, so they have to be written out here.
     *
     * <p>These are exactly {@code minecraft:block/block}'s values, because that is what the item model
     * inherited on 1.21.11 (where it was a {@code special} model parented to {@code block/cube_all}).
     * {@link gollorum.signpost.minecraft.rendering.PostItemRenderer} applies its own per-context
     * rotation on top of them, so the two have to be kept in step - using anything else here rotates
     * the rendered post away from the camera.
     */
    private void builtinEntity(String name) {
        getBuilder(name)
            .parent(new ModelFile.UncheckedModelFile(ResourceLocation.withDefaultNamespace("builtin/entity")))
            .transforms()
                .transform(ItemDisplayContext.GUI)
                    .rotation(30, 225, 0).scale(0.625f).end()
                .transform(ItemDisplayContext.GROUND)
                    .translation(0, 3, 0).scale(0.25f).end()
                .transform(ItemDisplayContext.FIXED)
                    .scale(0.5f).end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                    .rotation(75, 45, 0).translation(0, 2.5f, 0).scale(0.375f).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                    .rotation(0, 45, 0).scale(0.4f).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                    .rotation(0, 225, 0).scale(0.4f).end()
            .end();
    }
}
