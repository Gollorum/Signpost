package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.registry.RecipeRegistry;
import net.minecraft.data.*;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;

import java.util.function.Consumer;

public class WaystoneRecipe extends RecipeProvider {

    public WaystoneRecipe(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    protected void buildShapelessRecipes(Consumer<IFinishedRecipe> consumer) {
        ShapedRecipeBuilder.shaped(WaystoneBlock.INSTANCE)
            .define('s', Items.STONE)
            .define('e', Items.ENDER_PEARL)
            .pattern("sss")
            .pattern("ses")
            .pattern("sss")
            .unlockedBy("has_ender_pearl", has(Items.ENDER_PEARL))
            .unlockedBy("has_signpost", has(PostTag.Tag))
        .save(consumer);

        for(ModelWaystone.Variant v : ModelWaystone.variants) {
            new SingleItemRecipeBuilder(
                RecipeRegistry.CutWaystoneSerializer.get(),
                Ingredient.of(WaystoneTag.Tag),
                v.block,
                1
            ).unlocks("has_waystone", has(WaystoneTag.Tag))
            .save(consumer, new ResourceLocation(Signpost.MOD_ID, "cut_into_" + v.name));
        }
        new SingleItemRecipeBuilder(
            IRecipeSerializer.STONECUTTER,
            Ingredient.of(WaystoneTag.Tag),
            WaystoneBlock.INSTANCE,
            1
        ).unlocks("has_waystone", has(WaystoneTag.Tag))
            .save(consumer, new ResourceLocation(Signpost.MOD_ID, "cut_into_full_block"));
    }
}
