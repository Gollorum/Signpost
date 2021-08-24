package gollorum.signpost.minecraft.data;

import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.data.RecipeProvider;
import net.minecraft.data.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.IItemProvider;

import java.util.function.Consumer;

public class PostRecipe extends RecipeProvider {

    public PostRecipe(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    protected void registerRecipes(Consumer<IFinishedRecipe> consumer) {
        for(PostBlock.Variant variant : PostBlock.AllVariants) {
            postBuilder(variant.block, variant.type).build(consumer);
        }
    }

    private ShapedRecipeBuilder postBuilder(IItemProvider block, PostBlock.ModelType type) {
        return ShapedRecipeBuilder.shapedRecipe(block, 2)
            .key('s', type.signIngredient.get())
            .key('b', type.baseIngredient.get())
            .patternLine("s")
            .patternLine("s")
            .patternLine("b")
            .addCriterion("has_sign", hasItem(ItemTags.SIGNS))
            .addCriterion("has_signpost", hasItem(PostTag.Tag))
            .addCriterion("has_waystone", hasItem(WaystoneBlock.INSTANCE))
            .setGroup("Signpost");
    }

}
