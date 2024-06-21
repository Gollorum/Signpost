package gollorum.signpost.data;

import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.ItemLike;

import static net.minecraft.data.recipes.RecipeProvider.has;

public class PostRecipe {

    public static void build(RecipeOutput consumer) {
        for(PostBlock.Variant variant : PostBlock.AllVariants) {
            postBuilder(variant.getBlock(), variant.type).save(consumer);
        }
    }

    private static ShapedRecipeBuilder postBuilder(ItemLike block, PostBlock.ModelType type) {
        return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, block, 2)
            .define('s', type.signIngredient.get())
            .define('b', type.baseIngredient.get())
            .pattern("s")
            .pattern("s")
            .pattern("b")
            .unlockedBy("has_sign", has(ItemTags.SIGNS))
            .unlockedBy("has_signpost", has(gollorum.signpost.data.ItemTags.SignpostTag))
            .unlockedBy("has_waystone", has(WaystoneBlock.getInstance()))
            .group("Signpost");
    }

}
