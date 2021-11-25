package gollorum.signpost.minecraft.data;

import gollorum.signpost.minecraft.registry.ItemRegistry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.data.RecipeProvider;
import net.minecraft.data.ShapedRecipeBuilder;
import net.minecraft.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraftforge.common.data.ForgeRecipeProvider;

import java.util.function.Consumer;

public class WrenchRecipe extends RecipeProvider {

    public WrenchRecipe(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    protected void buildShapelessRecipes(Consumer<IFinishedRecipe> consumer) {
        ShapedRecipeBuilder.shaped(ItemRegistry.WRENCH.get(), 1)
            .define('s', ItemTags.SIGNS)
            .define('i', Items.IRON_INGOT)
            .pattern("i ")
            .pattern("ii")
            .pattern("s ")
            .unlocks("has_signpost", has(PostTag.Tag))
            .save(consumer);
    }

}
