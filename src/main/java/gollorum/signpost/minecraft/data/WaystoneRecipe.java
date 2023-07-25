package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.registry.RecipeRegistry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.function.Consumer;

public class WaystoneRecipe extends RecipeProvider {

    public WaystoneRecipe(PackOutput packOutput) {
        super(packOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, WaystoneBlock.getInstance())
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
                RecipeCategory.DECORATIONS,
                RecipeRegistry.CutWaystoneSerializer.get(),
                Ingredient.of(WaystoneTag.Tag),
                v.getBlock(),
                1
            ).unlockedBy("has_waystone", has(WaystoneTag.Tag))
            .save(consumer, new ResourceLocation(Signpost.MOD_ID, "cut_into_" + v.name));
        }
        new SingleItemRecipeBuilder(
            RecipeCategory.DECORATIONS,
            RecipeRegistry.CutWaystoneSerializer.get(),
            Ingredient.of(WaystoneTag.Tag),
            WaystoneBlock.getInstance(),
            1
        ).unlockedBy("has_waystone", has(WaystoneTag.Tag))
            .save(consumer, new ResourceLocation(Signpost.MOD_ID, "cut_into_full_block"));
    }
}
