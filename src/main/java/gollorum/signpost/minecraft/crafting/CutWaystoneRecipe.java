package gollorum.signpost.minecraft.crafting;

import com.google.gson.JsonObject;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.utils.serialization.StringSerializer;
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
        public CutWaystoneRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            String group = JSONUtils.getAsString(json, "group", "");
            Ingredient ingredient;
            if (JSONUtils.isArrayNode(json, "ingredient")) {
                ingredient = Ingredient.fromJson(JSONUtils.getAsJsonArray(json, "ingredient"));
            } else {
                ingredient = Ingredient.fromJson(JSONUtils.getAsJsonArray(json, "ingredient"));
            }

            String resultLoc = JSONUtils.getAsString(json, "result");
            int count = JSONUtils.getAsInt(json, "count");
            ItemStack result = new ItemStack(Registry.ITEM.get(new ResourceLocation(resultLoc)), count);
            return isAllowed(result)
                ? new CutWaystoneRecipe(recipeId, group, ingredient, result)
                : null;
        }

        @Override
        public CutWaystoneRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            String group = StringSerializer.instance.read(buffer);
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            ItemStack result = buffer.readItem();
            return new CutWaystoneRecipe(recipeId, group, ingredient, result);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, CutWaystoneRecipe recipe) {
            StringSerializer.instance.write(recipe.group, buffer);
            recipe.ingredient.toNetwork(buffer);
            buffer.writeItem(recipe.result);
        }

    }

}
