package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.registry.RecipeRegistry;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Consumer;

import static net.minecraft.data.recipes.RecipeProvider.has;

public class WaystoneRecipe {

    public static void build(Consumer<FinishedRecipe> consumer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, WaystoneBlock.getInstance())
            .define('s', Items.STONE)
            .define('e', Items.ENDER_PEARL)
            .pattern("sss")
            .pattern("ses")
            .pattern("sss")
            .unlockedBy("has_ender_pearl", has(Items.ENDER_PEARL))
            .unlockedBy("has_signpost", has(gollorum.signpost.minecraft.data.ItemTags.SignpostTag))
        .save(consumer);

        for(ModelWaystone.Variant v : ModelWaystone.variants) {
            new SingleItemRecipeBuilder(
                RecipeCategory.DECORATIONS,
                RecipeRegistry.CutWaystoneSerializer.get(),
                Ingredient.of(gollorum.signpost.minecraft.data.ItemTags.WaystoneTag),
                v.getBlock(),
                1
            ).unlockedBy("has_waystone", has(gollorum.signpost.minecraft.data.ItemTags.WaystoneTag))
            .save(consumer, new ResourceLocation(Signpost.MOD_ID, "cut_into_" + v.name));
        }
        new SingleItemRecipeBuilder(
            RecipeCategory.DECORATIONS,
            RecipeRegistry.CutWaystoneSerializer.get(),
            Ingredient.of(gollorum.signpost.minecraft.data.ItemTags.WaystoneTag),
            WaystoneBlock.getInstance(),
            1
        ).unlockedBy("has_waystone", has(gollorum.signpost.minecraft.data.ItemTags.WaystoneTag))
            .save(consumer, new ResourceLocation(Signpost.MOD_ID, "cut_into_full_block"));
    }
}
