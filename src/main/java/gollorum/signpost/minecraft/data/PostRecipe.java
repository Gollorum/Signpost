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
        switch (type) {
            case Acacia:
                return Ingredient.fromItems(Items.ACACIA_SIGN);
            case Birch:
                return Ingredient.fromItems(Items.BIRCH_SIGN);
            case Iron:
            case Stone:
                return Ingredient.fromTag(ItemTags.SIGNS);
            case Jungle:
                return Ingredient.fromItems(Items.JUNGLE_SIGN);
            case Oak:
                return Ingredient.fromItems(Items.OAK_SIGN);
            case DarkOak:
                return Ingredient.fromItems(Items.DARK_OAK_SIGN);
            case Spruce:
                return Ingredient.fromItems(Items.SPRUCE_SIGN);
            case Warped:
                return Ingredient.fromItems(Items.WARPED_SIGN);
            case Crimson:
                return Ingredient.fromItems(Items.CRIMSON_SIGN);
            default: throw new RuntimeException("Signpost type " + type + " is not supported");
        }
    }

    private static Ingredient baseFor(Post.ModelType type) {
        switch (type) {
            case Acacia:
                return Ingredient.fromTag(ItemTags.ACACIA_LOGS);
            case Birch:
                return Ingredient.fromTag(ItemTags.BIRCH_LOGS);
            case Iron:
                return Ingredient.fromItems(Items.IRON_INGOT);
            case Stone:
                return Ingredient.fromItems(Items.STONE);
            case Jungle:
                return Ingredient.fromTag(ItemTags.JUNGLE_LOGS);
            case Oak:
                return Ingredient.fromTag(ItemTags.OAK_LOGS);
            case DarkOak:
                return Ingredient.fromTag(ItemTags.DARK_OAK_LOGS);
            case Spruce:
                return Ingredient.fromTag(ItemTags.SPRUCE_LOGS);
            case Warped:
                return Ingredient.fromTag(ItemTags.WARPED_STEMS);
            case Crimson:
                return Ingredient.fromTag(ItemTags.CRIMSON_STEMS);
            default: throw new RuntimeException("Signpost type " + type + " is not supported");
        }
    }

}
