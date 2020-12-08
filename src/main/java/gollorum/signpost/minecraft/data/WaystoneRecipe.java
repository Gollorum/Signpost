package gollorum.signpost.minecraft.data;

import gollorum.signpost.minecraft.block.Waystone;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.data.RecipeProvider;
import net.minecraft.data.ShapedRecipeBuilder;
import net.minecraft.item.Items;

import java.util.function.Consumer;

public class WaystoneRecipe extends RecipeProvider {

    public WaystoneRecipe(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    protected void registerRecipes(Consumer<IFinishedRecipe> consumer) {
        ShapedRecipeBuilder.shapedRecipe(Waystone.INSTANCE, 1)
            .key('s', Items.STONE)
            .key('e', Items.ENDER_PEARL)
            .patternLine("sss")
            .patternLine("ses")
            .patternLine("sss")
            .addCriterion("has_ender_pearl", hasItem(Items.ENDER_PEARL))
            .addCriterion("has_signpost", hasItem(PostTag.Tag))
        .build(consumer);
    }
}
