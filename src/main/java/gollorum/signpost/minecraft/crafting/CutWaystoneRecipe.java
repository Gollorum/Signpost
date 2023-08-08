package gollorum.signpost.minecraft.crafting;

import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.config.Config;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

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

    @Override
    public @NotNull ItemStack assemble(@NotNull Container container, RegistryAccess registryAccess) {
        ItemStack ret = super.assemble(container, registryAccess);
        ItemStack ingred = container.getItem(0);
        if(ingred.hasTag()) ret.setTag(ret.hasTag()
            ? ret.getTag().merge(ingred.getTag())
            : ingred.getTag().copy()
        );
        return ret;
    }

    public static class Serializer extends SingleItemRecipe.Serializer<CutWaystoneRecipe> {
        public Serializer() { super(CutWaystoneRecipe::new); }
    }

}
