package gollorum.signpost.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.crafting.CutWaystoneRecipe;
import gollorum.signpost.registry.ItemRegistry;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.concurrent.CompletableFuture;

public class Recipes extends RecipeProvider {

    public static class Runner extends RecipeProvider.Runner {

        protected Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> provider) {
            super(output, provider);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider provider, RecipeOutput recipeOutput) {
            return new Recipes(provider, recipeOutput);
        }

        @Override
        public String getName() {
            return "signpost";
        }
    }

    public Recipes(HolderLookup.Provider registryAccess, RecipeOutput output) {
        super(registryAccess, output);
    }

    @Override
    protected void buildRecipes() {
        registerBrush();
        registerPosts();
        registerWaystones();
        registerWrench();
    }

    private void registerBrush() {
        shaped(RecipeCategory.TOOLS, ItemRegistry.BRUSH.get())
            .define('w', net.minecraft.tags.ItemTags.WOOL)
            .define('i', Items.IRON_INGOT)
            .define('s', Items.STICK)
            .pattern("w")
            .pattern("i")
            .pattern("s")
            .unlockedBy("has_signpost", has(gollorum.signpost.data.ItemTags.SignpostTag))
            .save(output);
    }


    public void registerPosts() {
        for(PostBlock.Variant variant : PostBlock.AllVariants) {
            shaped(RecipeCategory.DECORATIONS, variant.getBlock(), 2)
                .define('s', variant.type.signIngredient.get())
                .define('b', variant.type.baseIngredient.get())
                .pattern("s")
                .pattern("s")
                .pattern("b")
                .unlockedBy("has_sign", has(net.minecraft.tags.ItemTags.SIGNS))
                .unlockedBy("has_signpost", has(gollorum.signpost.data.ItemTags.SignpostTag))
                .unlockedBy("has_waystone", has(WaystoneBlock.getInstance()))
                .group("Signpost")
                .save(output);
        }
    }

    public void registerWaystones() {
        shaped(RecipeCategory.DECORATIONS, WaystoneBlock.getInstance())
            .define('s', Items.STONE)
            .define('e', Items.ENDER_PEARL)
            .pattern("sss")
            .pattern("ses")
            .pattern("sss")
            .unlockedBy("has_ender_pearl", has(Items.ENDER_PEARL))
            .unlockedBy("has_signpost", has(ItemTags.SignpostTag))
            .save(output);

        for(ModelWaystone.Variant v : ModelWaystone.variants) {
            new SingleItemRecipeBuilder(
                RecipeCategory.DECORATIONS,
                CutWaystoneRecipe::new,
                tag(ItemTags.WaystoneTag),
                v.getBlock(),
                1
            ).unlockedBy("has_waystone", has(ItemTags.WaystoneTag))
                .save(output, ResourceKey.create(Registries.RECIPE, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "cut_into_" + v.name)));
        }
        new SingleItemRecipeBuilder(
            RecipeCategory.DECORATIONS,
            CutWaystoneRecipe::new,
            tag(ItemTags.WaystoneTag),
            WaystoneBlock.getInstance(),
            1
        ).unlockedBy("has_waystone", has(ItemTags.WaystoneTag))
            .save(output, ResourceKey.create(Registries.RECIPE, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "cut_into_full_block")));
    }

    public void registerWrench() {
        shaped(RecipeCategory.TOOLS, ItemRegistry.WRENCH.get(), 1)
            .define('s', net.minecraft.tags.ItemTags.SIGNS)
            .define('i', Items.IRON_INGOT)
            .pattern("i ")
            .pattern("ii")
            .pattern("s ")
            .unlockedBy("has_signpost", has(gollorum.signpost.data.ItemTags.SignpostTag))
            .save(output);
    }
}
