package gollorum.signpost.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.migration.LegacyPostTypes;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.crafting.CutWaystoneRecipe;
import gollorum.signpost.registry.ItemRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.concurrent.CompletableFuture;

public class Recipes extends RecipeProvider {

    /**
     * 1.21.1's {@code buildRecipes} is handed only the {@link RecipeOutput}, and there is no
     * {@code RecipeProvider.Runner} to split construction from generation, so the registry lookup the
     * post model types need is captured on the way through {@link #run}.
     */
    private HolderLookup.Provider registries;

    public Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected CompletableFuture<?> run(CachedOutput output, HolderLookup.Provider registries) {
        this.registries = registries;
        return super.run(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        registerBrush(output);
        registerPosts(output);
        registerWaystones(output);
        registerWrench(output);
    }

    private void registerBrush(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.BRUSH.get())
            .define('w', net.minecraft.tags.ItemTags.WOOL)
            .define('i', Items.IRON_INGOT)
            .define('s', Items.STICK)
            .pattern("w")
            .pattern("i")
            .pattern("s")
            .unlockedBy("has_signpost", has(ItemTags.SignpostTag))
            .save(output);
    }

    public void registerPosts(RecipeOutput output) {
        for (var variant : PostModelTypes.getAll(registries.lookupOrThrow(Registries.ITEM))) {
            // The crafted post carries its model type in its data components, which vanilla's
            // ShapedRecipeBuilder cannot express on this version - see StackResultShapedRecipeBuilder.
            StackResultShapedRecipeBuilder.shaped(
                    RecipeCategory.DECORATIONS, variant.value().getItemStack(variant.getKey(), 2))
                .define('s', variant.signIngredient())
                .define('b', variant.baseIngredient())
                .pattern("s")
                .pattern("s")
                .pattern("b")
                .unlockedBy("has_sign", has(net.minecraft.tags.ItemTags.SIGNS))
                .unlockedBy("has_signpost", has(ItemTags.SignpostTag))
                .unlockedBy("has_waystone", has(WaystoneBlock.getInstance()))
                .group("Signpost")
                .save(output, ResourceLocation.fromNamespaceAndPath(
                    Signpost.MOD_ID, LegacyPostTypes.recipeIdFor(variant.name())));
        }
    }

    public void registerWaystones(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, WaystoneBlock.getInstance())
            .define('s', Items.STONE)
            .define('e', Items.ENDER_PEARL)
            .pattern("sss")
            .pattern("ses")
            .pattern("sss")
            .unlockedBy("has_ender_pearl", has(Items.ENDER_PEARL))
            .unlockedBy("has_signpost", has(ItemTags.SignpostTag))
            .save(output);

        for (ModelWaystone.Variant v : ModelWaystone.variants) {
            new SingleItemRecipeBuilder(
                RecipeCategory.DECORATIONS,
                CutWaystoneRecipe::new,
                Ingredient.of(ItemTags.WaystoneTag),
                v.getBlock(),
                1
            ).unlockedBy("has_waystone", has(ItemTags.WaystoneTag))
                .save(output, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "cut_into_" + v.name));
        }
        new SingleItemRecipeBuilder(
            RecipeCategory.DECORATIONS,
            CutWaystoneRecipe::new,
            Ingredient.of(ItemTags.WaystoneTag),
            WaystoneBlock.getInstance(),
            1
        ).unlockedBy("has_waystone", has(ItemTags.WaystoneTag))
            .save(output, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "cut_into_full_block"));
    }

    public void registerWrench(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.WRENCH.get(), 1)
            .define('s', net.minecraft.tags.ItemTags.SIGNS)
            .define('i', Items.IRON_INGOT)
            .pattern("i ")
            .pattern("ii")
            .pattern("s ")
            .unlockedBy("has_signpost", has(ItemTags.SignpostTag))
            .save(output);
    }
}
