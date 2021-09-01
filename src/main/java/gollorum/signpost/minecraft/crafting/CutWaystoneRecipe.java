package gollorum.signpost.minecraft.crafting;

import com.google.gson.JsonObject;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.utils.serialization.StringSerializer;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistryEntry;

public class CutWaystoneRecipe extends StonecutterRecipe {

    public static final String RegistryName = "cut_waystone";

    public CutWaystoneRecipe(ResourceLocation id, String group, Ingredient ingredient, ItemStack result) {
        super(id, group, ingredient, result);
    }

    @Override
    public boolean matches(Container inv, Level world) {
        return super.matches(inv, world) && isAllowed(result);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    private static boolean isAllowed(ItemStack result) {
        if(!(result.getItem() instanceof BlockItem)) return true;
        Block block = ((BlockItem)result.getItem()).getBlock();
        if(!(block instanceof ModelWaystone)) return true;
        return Config.Server.allowedWaystones.get().contains(((ModelWaystone)block).variant.name);
    }

    public static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<CutWaystoneRecipe> {

        public static Serializer INSTANCE = new Serializer();

        @Override
        public CutWaystoneRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            String group = GsonHelper.getAsString(json, "group", "");
            Ingredient ingredient;
            if (GsonHelper.isArrayNode(json, "ingredient")) {
                ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            } else {
                ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            }

            String resultLoc = GsonHelper.getAsString(json, "result");
            int count = GsonHelper.getAsInt(json, "count");
            ItemStack result = new ItemStack(Registry.ITEM.get(new ResourceLocation(resultLoc)), count);
            return isAllowed(result)
                ? new CutWaystoneRecipe(recipeId, group, ingredient, result)
                : null;
        }

        @Override
        public CutWaystoneRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            String group = StringSerializer.instance.read(buffer);
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            ItemStack result = buffer.readItem();
            return new CutWaystoneRecipe(recipeId, group, ingredient, result);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, CutWaystoneRecipe recipe) {
            StringSerializer.instance.write(recipe.group, buffer);
            recipe.ingredient.toNetwork(buffer);
            buffer.writeItem(recipe.result);
        }

    }

}
