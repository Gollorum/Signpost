package gollorum.signpost.minecraft.data;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;

import java.util.function.Consumer;

public class Recipes extends RecipeProvider {

    public Recipes(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
        BrushRecipe.build(consumer);
        PostRecipe.build(consumer);
        WaystoneRecipe.build(consumer);
        WrenchRecipe.build(consumer);
    }

}
