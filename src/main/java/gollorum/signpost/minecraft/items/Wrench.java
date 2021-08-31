package gollorum.signpost.minecraft.items;

import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.context.UseOnContext;

public class Wrench extends TieredItem {

    public static final String registryName = "tool";

    public Wrench(CreativeModeTab itemGroup) {
        super(Tiers.IRON, new Properties().tab(itemGroup));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return TileEntityUtils.findTileEntity(context.getLevel(), context.getClickedPos(), PostTile.class)
            .map(tile -> PostBlock.onActivate(tile, context.getLevel(), context.getPlayer(), context.getHand()))
            .orElse(InteractionResult.PASS);
    }
}
