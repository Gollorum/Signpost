package gollorum.signpost.minecraft.data;

import gollorum.signpost.minecraft.registry.ItemRegistry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.data.RecipeProvider;
import net.minecraft.data.ShapedRecipeBuilder;
import net.minecraft.item.Items;
import net.minecraft.tags.ItemTags;

import java.util.function.Consumer;

public class WrenchRecipe extends RecipeProvider {

    public WrenchRecipe(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    protected void registerRecipes(Consumer<IFinishedRecipe> consumer) {
        ShapedRecipeBuilder.shapedRecipe(ItemRegistry.WRENCH.get(), 1)
            .key('s', ItemTags.SIGNS)
            .key('i', Items.IRON_INGOT)
            .patternLine("i ")
            .patternLine("ii")
            .patternLine("s ")
            .addCriterion("has_signpost", hasItem(PostTag.Tag))
            .build(consumer);
    }

}
