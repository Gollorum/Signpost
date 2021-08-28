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
    protected void buildShapelessRecipes(Consumer<IFinishedRecipe> consumer) {
        for(PostBlock.Variant variant : PostBlock.AllVariants) {
            postBuilder(variant.block, variant.type).save(consumer);
        }
    }

    private ShapedRecipeBuilder postBuilder(IItemProvider block, PostBlock.ModelType type) {
        return ShapedRecipeBuilder.shaped(block, 2)
            .define('s', type.signIngredient.get())
            .define('b', type.baseIngredient.get())
            .pattern("s")
            .pattern("s")
            .pattern("b")
            .unlockedBy("has_sign", has(ItemTags.SIGNS))
            .unlockedBy("has_signpost", has(PostTag.Tag))
            .unlockedBy("has_waystone", has(WaystoneBlock.INSTANCE))
            .group("Signpost");
    }

}
