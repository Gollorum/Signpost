package gollorum.signpost.minecraft.crafting;

import com.google.gson.JsonObject;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.config.Config;
import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.StonecuttingRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;

public class CutWaystoneRecipe extends StonecuttingRecipe {

    public static final String RegistryName = "cut_waystone";

    public CutWaystoneRecipe(ResourceLocation id, String group, Ingredient ingredient, ItemStack result) {
        super(id, group, ingredient, result);
    }

    @Override
    public boolean matches(IInventory inv, World world) {
        return super.matches(inv, world) && isAllowed(result);
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    private static boolean isAllowed(ItemStack result) {
        if(!(result.getItem() instanceof BlockItem)) return true;
        Block block = ((BlockItem)result.getItem()).getBlock();
        if(!(block instanceof ModelWaystone)) return true;
        return Config.Server.allowedWaystones.get().contains(((ModelWaystone)block).variant.name);
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<CutWaystoneRecipe> {

        public static Serializer INSTANCE = new Serializer();

        @Override
        public CutWaystoneRecipe read(ResourceLocation recipeId, JsonObject json) {
            String group = JSONUtils.getString(json, "group", "");
            Ingredient ingredient;
            if (JSONUtils.isJsonArray(json, "ingredient")) {
                ingredient = Ingredient.deserialize(JSONUtils.getJsonArray(json, "ingredient"));
            } else {
                ingredient = Ingredient.deserialize(JSONUtils.getJsonObject(json, "ingredient"));
            }

            String resultLoc = JSONUtils.getString(json, "result");
            int count = JSONUtils.getInt(json, "count");
            ItemStack result = new ItemStack(Registry.ITEM.getOrDefault(new ResourceLocation(resultLoc)), count);
            return isAllowed(result)
                ? new CutWaystoneRecipe(recipeId, group, ingredient, result)
                : null;
        }

        @Override
        public CutWaystoneRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
            String group = buffer.readString(32767);
            Ingredient ingredient = Ingredient.read(buffer);
            ItemStack result = buffer.readItemStack();
            return new CutWaystoneRecipe(recipeId, group, ingredient, result);
        }

        @Override
        public void write(PacketBuffer buffer, CutWaystoneRecipe recipe) {
            buffer.writeString(recipe.group);
            recipe.ingredient.write(buffer);
            buffer.writeItemStack(recipe.result);
        }

    }

}
