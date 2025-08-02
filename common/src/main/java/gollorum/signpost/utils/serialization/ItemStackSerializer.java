package gollorum.signpost.utils.serialization;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;

public final class ItemStackSerializer {

    public static final MapCodec<ItemStack> CODEC = ItemStack.OPTIONAL_CODEC.fieldOf("ItemStack");

}