package gollorum.signpost.minecraft.items;

import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class GenerationWand extends Item {

    public static final String registryName = "generation_wand";

    public GenerationWand() {
        super(new Properties());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return TileEntityUtils.findTileEntity(context.getLevel(), context.getClickedPos(), PostTile.getBlockEntityType())
                .map(tile -> PostBlock.onActivate(tile, context.getLevel(), context.getPlayer(), context.getHand()))
                .orElse(InteractionResult.PASS);
    }
}
