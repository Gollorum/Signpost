package gollorum.signpost.minecraft.data;

import gollorum.signpost.minecraft.block.Post;
import gollorum.signpost.minecraft.block.Waystone;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.data.RecipeProvider;
import net.minecraft.data.ShapedRecipeBuilder;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.IItemProvider;

import java.util.function.Consumer;

public class PostRecipe extends RecipeProvider {

    public PostRecipe(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    protected void registerRecipes(Consumer<IFinishedRecipe> consumer) {
        for(Post.Variant variant : Post.AllVariants) {
            postBuilder(variant.block, variant.type).build(consumer);
        }
    }

    private ShapedRecipeBuilder postBuilder(IItemProvider block, Post.ModelType type) {
        return ShapedRecipeBuilder.shapedRecipe(block, 2)
            .key('s', signFor(type))
            .key('b', baseFor(type))
            .patternLine("s")
            .patternLine("s")
            .patternLine("b")
            .addCriterion("has_sign", hasItem(ItemTags.SIGNS))
            .addCriterion("has_signpost", hasItem(PostTag.Tag))
            .addCriterion("has_waystone", hasItem(Waystone.INSTANCE))
            .setGroup("Signpost");
    }

    private static Ingredient signFor(Post.ModelType type) {
        if(Post.ModelType.signIngredientForType.containsKey(type))
            return Post.ModelType.signIngredientForType.get(type).get();
        else throw new RuntimeException("Signpost type " + type + " is not supported");
    }

    private static Ingredient baseFor(Post.ModelType type) {
        if(Post.ModelType.baseIngredientForType.containsKey(type))
            return Post.ModelType.baseIngredientForType.get(type).get();
        else throw new RuntimeException("Signpost type " + type + " is not supported");
    }

}
