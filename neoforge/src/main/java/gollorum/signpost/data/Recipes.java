package gollorum.signpost.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.crafting.CutWaystoneRecipe;
import gollorum.signpost.minecraft.data.PostData;
import gollorum.signpost.registry.ItemRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Recipes extends RecipeProvider {

    protected Recipes(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
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
            .unlockedBy("has_signpost", has(ItemTags.SignpostTag))
            .save(output);
    }


    public void registerPosts() {
        for(var variant : PostModelTypes.getAll(registries.lookupOrThrow(Registries.ITEM))) {
            shaped(RecipeCategory.DECORATIONS, variant.value().getItemStack(variant.getKey(), 2))
                .define('s', variant.signIngredient())
                .define('b', variant.baseIngredient())
                .pattern("s")
                .pattern("s")
                .pattern("b")
                .unlockedBy("has_sign", has(net.minecraft.tags.ItemTags.SIGNS))
                .unlockedBy("has_signpost", has(ItemTags.SignpostTag))
                .unlockedBy("has_waystone", has(WaystoneBlock.getInstance()))
                .group("Signpost")
                .save(output, ResourceKey.create(Registries.RECIPE, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, variant.name())));
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
                .save(output, ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "cut_into_" + v.name)));
        }
        new SingleItemRecipeBuilder(
            RecipeCategory.DECORATIONS,
            CutWaystoneRecipe::new,
            tag(ItemTags.WaystoneTag),
            WaystoneBlock.getInstance(),
            1
        ).unlockedBy("has_waystone", has(ItemTags.WaystoneTag))
            .save(output, ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "cut_into_full_block")));
    }

    public void registerWrench() {
        shaped(RecipeCategory.TOOLS, ItemRegistry.WRENCH.get(), 1)
            .define('s', net.minecraft.tags.ItemTags.SIGNS)
            .define('i', Items.IRON_INGOT)
            .pattern("i ")
            .pattern("ii")
            .pattern("s ")
            .unlockedBy("has_signpost", has(ItemTags.SignpostTag))
            .save(output);
    }
    
    public static class Runner extends net.minecraft.data.recipes.RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(output, lookupProvider);
        }

        @Override
        protected net.minecraft.data.recipes.RecipeProvider createRecipeProvider(HolderLookup.Provider registries, net.minecraft.data.recipes.RecipeOutput output) {
            return new Recipes(registries, output);
        }

        @Override
        public String getName() {
            return "Signpost Recipes";
        }
    }
}
