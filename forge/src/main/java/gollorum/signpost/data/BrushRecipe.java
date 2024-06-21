package gollorum.signpost.data;

import gollorum.signpost.registry.ItemRegistry;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

import static net.minecraft.data.recipes.RecipeProvider.has;

public class BrushRecipe {

    public static void build(RecipeOutput consumer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.BRUSH.get())
            .define('w', ItemTags.WOOL)
            .define('i', Items.IRON_INGOT)
            .define('s', Items.STICK)
            .pattern("w")
            .pattern("i")
            .pattern("s")
            .unlockedBy("has_signpost", has(gollorum.signpost.data.ItemTags.SignpostTag))
            .save(consumer);
    }

}
