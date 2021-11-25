package gollorum.signpost.minecraft.data;

import gollorum.signpost.minecraft.registry.ItemRegistry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.data.RecipeProvider;
import net.minecraft.data.ShapedRecipeBuilder;
import net.minecraft.item.Items;
import net.minecraft.tags.ItemTags;

import java.util.function.Consumer;

public class BrushRecipe extends RecipeProvider {

    public BrushRecipe(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    protected void buildShapelessRecipes(Consumer<IFinishedRecipe> consumer) {
        ShapedRecipeBuilder.shaped(ItemRegistry.BRUSH.get(), 1)
            .define('w', ItemTags.WOOL)
            .define('i', Items.IRON_INGOT)
            .define('s', Items.STICK)
            .pattern("w")
            .pattern("i")
            .pattern("s")
            .unlocks("has_signpost", has(PostTag.Tag))
            .save(consumer);
    }

}
