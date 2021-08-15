package gollorum.signpost.minecraft.data;

import gollorum.signpost.minecraft.block.Waystone;
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
    protected void registerRecipes(Consumer<IFinishedRecipe> consumer) {
        ShapedRecipeBuilder.shapedRecipe(ItemRegistry.BRUSH.get(), 1)
            .key('w', ItemTags.WOOL)
            .key('i', Items.IRON_INGOT)
            .key('s', Items.STICK)
            .patternLine("w")
            .patternLine("i")
            .patternLine("s")
            .addCriterion("has_signpost", hasItem(PostTag.Tag))
            .build(consumer);
    }

}
