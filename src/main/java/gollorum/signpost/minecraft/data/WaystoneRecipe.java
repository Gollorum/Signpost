package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.Waystone;
import gollorum.signpost.minecraft.crafting.CycleWaystoneModelRecipe;
import gollorum.signpost.minecraft.registry.RecipeRegistry;
import net.minecraft.data.*;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;

import java.util.function.Consumer;

public class WaystoneRecipe extends RecipeProvider {

    public WaystoneRecipe(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    protected void registerRecipes(Consumer<IFinishedRecipe> consumer) {
        ShapedRecipeBuilder.shapedRecipe(Waystone.INSTANCE)
            .key('s', Items.STONE)
            .key('e', Items.ENDER_PEARL)
            .patternLine("sss")
            .patternLine("ses")
            .patternLine("sss")
            .addCriterion("has_ender_pearl", hasItem(Items.ENDER_PEARL))
            .addCriterion("has_signpost", hasItem(PostTag.Tag))
        .build(consumer);

        CustomRecipeBuilder.customRecipe(RecipeRegistry.CycleWaystoneModelSerializer.get()).
            build(consumer, new ResourceLocation(Signpost.MOD_ID, CycleWaystoneModelRecipe.RegistryName).toString());
    }
}
