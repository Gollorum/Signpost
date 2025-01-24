package gollorum.signpost.minecraft.crafting;

import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.config.IConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class CutWaystoneRecipe extends StonecutterRecipe {

    public static final String RegistryName = "cut_waystone";

    // TODO DS: ID?
    public CutWaystoneRecipe(String group, Ingredient ingredient, ItemStack result) {
        super(group, ingredient, result);
    }

    @Override
    public boolean matches(SingleRecipeInput inv, Level world) {
        return super.matches(inv, world) && isAllowed(result());
    }

    private static boolean isAllowed(ItemStack result) {
        if(!(result.getItem() instanceof BlockItem)) return true;
        Block block = ((BlockItem)result.getItem()).getBlock();
        if(!(block instanceof ModelWaystone)) return true;
        return IConfig.IServer.getInstance().allowedWaystones().contains(((ModelWaystone)block).variant.name);
    }

    @Override
    public @NotNull ItemStack assemble(SingleRecipeInput container, HolderLookup.Provider registryAccess) {
        ItemStack ret = super.assemble(container, registryAccess);
        ItemStack ingred = container.getItem(0);
        if(ingred.has(DataComponents.CUSTOM_DATA)) {
            var data = ingred.get(DataComponents.CUSTOM_DATA).copyTag();
            if (ret.has(DataComponents.CUSTOM_DATA)) {
                data = ret.get(DataComponents.CUSTOM_DATA).copyTag().merge(data);
            }
            ret.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        return ret;
    }

    public static class Serializer extends SingleItemRecipe.Serializer<CutWaystoneRecipe> {
        public Serializer() { super(CutWaystoneRecipe::new); }
    }

}
