package gollorum.signpost.minecraft.crafting;

import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.config.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class CutWaystoneRecipe extends StonecutterRecipe {

    public static final String RegistryName = "cut_waystone";

    public CutWaystoneRecipe(ResourceLocation id, String group, Ingredient ingredient, ItemStack result) {
        super(id, group, ingredient, result);
    }

    @Override
    public boolean matches(Container inv, Level world) {
        return super.matches(inv, world) && isAllowed(result);
    }

    private static boolean isAllowed(ItemStack result) {
        if(!(result.getItem() instanceof BlockItem)) return true;
        Block block = ((BlockItem)result.getItem()).getBlock();
        if(!(block instanceof ModelWaystone)) return true;
        return Config.Server.allowedWaystones.get().contains(((ModelWaystone)block).variant.name);
    }

    public static class Serializer extends SingleItemRecipe.Serializer<CutWaystoneRecipe> {
        public Serializer() { super(CutWaystoneRecipe::new); }
    }

}
