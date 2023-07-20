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

    public Wrench() {
        super(Tiers.IRON, new Properties());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return context.getLevel().getBlockEntity(context.getClickedPos(), PostTile.getBlockEntityType())
            .map(tile -> PostBlock.onActivate(tile, context.getLevel(), context.getPlayer(), context.getHand()))
            .orElse(InteractionResult.PASS);
    }
}
