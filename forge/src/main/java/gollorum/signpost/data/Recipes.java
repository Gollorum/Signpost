package gollorum.signpost.data;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

public class Recipes extends RecipeProvider {

    public Recipes(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        BrushRecipe.build(output);
        PostRecipe.build(output);
        WaystoneRecipe.build(output);
        WrenchRecipe.build(output);
    }

}
