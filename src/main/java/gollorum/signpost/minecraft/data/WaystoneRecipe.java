package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.registry.RecipeRegistry;
import net.minecraft.data.*;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;

import java.util.function.Consumer;

public class WaystoneRecipe extends RecipeProvider {

    public WaystoneRecipe(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    protected void registerRecipes(Consumer<IFinishedRecipe> consumer) {
        ShapedRecipeBuilder.shapedRecipe(WaystoneBlock.INSTANCE)
            .key('s', Items.STONE)
            .key('e', Items.ENDER_PEARL)
            .patternLine("sss")
            .patternLine("ses")
            .patternLine("sss")
            .addCriterion("has_ender_pearl", hasItem(Items.ENDER_PEARL))
            .addCriterion("has_signpost", hasItem(PostTag.Tag))
        .build(consumer);

        for(ModelWaystone.Variant v : ModelWaystone.variants) {
            new SingleItemRecipeBuilder(
                RecipeRegistry.CutWaystoneSerializer.get(),
                Ingredient.fromTag(WaystoneTag.Tag),
                v.block,
                1
            ).addCriterion("has_waystone", hasItem(WaystoneTag.Tag))
            .build(consumer, new ResourceLocation(Signpost.MOD_ID, "cut_into_" + v.name));
        }
        new SingleItemRecipeBuilder(
            IRecipeSerializer.STONECUTTING,
            Ingredient.fromTag(WaystoneTag.Tag),
            WaystoneBlock.INSTANCE,
            1
        ).addCriterion("has_waystone", hasItem(WaystoneTag.Tag))
            .build(consumer, new ResourceLocation(Signpost.MOD_ID, "cut_into_full_block"));
    }
}
