package gollorum.signpost.minecraft.data;

import gollorum.signpost.minecraft.registry.ItemRegistry;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

import static net.minecraft.data.recipes.RecipeProvider.has;

public class BrushRecipe {

    public static void build(Consumer<FinishedRecipe> consumer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.BRUSH.get())
            .define('w', ItemTags.WOOL)
            .define('i', Items.IRON_INGOT)
            .define('s', Items.STICK)
            .pattern("w")
            .pattern("i")
            .pattern("s")
            .unlockedBy("has_signpost", has(gollorum.signpost.minecraft.data.ItemTags.SignpostTag))
            .save(consumer);
    }

}
