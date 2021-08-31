package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.registry.RecipeRegistry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.function.Consumer;

public class WaystoneRecipe extends RecipeProvider {

    public WaystoneRecipe(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    protected void buildCraftingRecipes(Consumer<FinishedRecipe> consumer) {
        ShapedRecipeBuilder.shaped(WaystoneBlock.INSTANCE)
            .define('s', Items.STONE)
            .define('e', Items.ENDER_PEARL)
            .pattern("sss")
            .pattern("ses")
            .pattern("sss")
            .unlockedBy("has_ender_pearl", has(Items.ENDER_PEARL))
            .unlockedBy("has_signpost", has(PostTag.Tag))
        .save(consumer);

        for(ModelWaystone.Variant v : ModelWaystone.variants) {
            new SingleItemRecipeBuilder(
                RecipeRegistry.CutWaystoneSerializer.get(),
                Ingredient.of(WaystoneTag.Tag),
                v.block,
                1
            ).unlockedBy("has_waystone", has(WaystoneTag.Tag))
            .save(consumer, new ResourceLocation(Signpost.MOD_ID, "cut_into_" + v.name));
        }
        new SingleItemRecipeBuilder(
            RecipeSerializer.STONECUTTER,
            Ingredient.of(WaystoneTag.Tag),
            WaystoneBlock.INSTANCE,
            1
        ).unlockedBy("has_waystone", has(WaystoneTag.Tag))
            .save(consumer, new ResourceLocation(Signpost.MOD_ID, "cut_into_full_block"));
    }
}
