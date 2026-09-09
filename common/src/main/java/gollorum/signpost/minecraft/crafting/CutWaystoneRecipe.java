package gollorum.signpost.minecraft.crafting;

import com.mojang.serialization.MapCodec;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.minecraft.data.WaystoneHandleData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class CutWaystoneRecipe extends StonecutterRecipe {

    public static final String RegistryName = "cut_waystone";

    // 26.1 turned RecipeSerializer into a concrete record of the two codecs, so the
    // SingleItemRecipe.Serializer class this used to extend no longer exists.
    public static final MapCodec<CutWaystoneRecipe> MAP_CODEC = simpleMapCodec(CutWaystoneRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, CutWaystoneRecipe> STREAM_CODEC =
        simpleStreamCodec(CutWaystoneRecipe::new);
    public static final RecipeSerializer<CutWaystoneRecipe> SERIALIZER =
        new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public CutWaystoneRecipe(Recipe.CommonInfo commonInfo, Ingredient ingredient, ItemStackTemplate result) {
        super(commonInfo, ingredient, result);
    }

    @Override
    public boolean matches(SingleRecipeInput inv, Level world) {
        return super.matches(inv, world) && isAllowed(result());
    }

    private static boolean isAllowed(ItemStackTemplate result) {
        if(!(result.item().value() instanceof BlockItem blockItem)) return true;
        Block block = blockItem.getBlock();
        if(!(block instanceof ModelWaystone)) return true;
        return IConfig.IServer.getInstance().allowedWaystones().contains(((ModelWaystone)block).variant.name);
    }

    @Override
    public @NotNull ItemStack assemble(SingleRecipeInput container) {
        ItemStack ret = super.assemble(container);
        ItemStack ingred = container.item();
        if(ingred.has(WaystoneHandleData.TYPE)) {
            var data = ingred.get(WaystoneHandleData.TYPE);
            ret.set(WaystoneHandleData.TYPE, data);
        }
        return ret;
    }

}
