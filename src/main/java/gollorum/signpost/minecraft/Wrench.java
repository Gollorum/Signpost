package gollorum.signpost.minecraft;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemTier;
import net.minecraft.item.ToolItem;

import java.util.HashSet;

public class Wrench extends ToolItem {

    public static final String registryName = "tool";

    public Wrench(ItemGroup itemGroup) {
        super(0, -3, ItemTier.IRON, new HashSet<>(), new Properties().group(itemGroup));
    }
}
