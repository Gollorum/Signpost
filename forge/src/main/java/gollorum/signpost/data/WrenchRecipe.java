package gollorum.signpost.data;

import gollorum.signpost.registry.ItemRegistry;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

import static net.minecraft.data.recipes.RecipeProvider.has;

public class WrenchRecipe {

    public static void build(RecipeOutput consumer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.WRENCH.get(), 1)
            .define('s', ItemTags.SIGNS)
            .define('i', Items.IRON_INGOT)
            .pattern("i ")
            .pattern("ii")
            .pattern("s ")
            .unlockedBy("has_signpost", has(gollorum.signpost.data.ItemTags.SignpostTag))
            .save(consumer);
    }

}
