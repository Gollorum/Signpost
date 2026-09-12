package gollorum.signpost.minecraft.items;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class Wrench extends Item {

    public static final String registryName = "tool";

    public Wrench() {
        super(new Properties()
            .stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return context.getLevel().getBlockEntity(context.getClickedPos(), PostTile.getBlockEntityType())
            .map(tile -> PostBlock.onActivate(tile, context.getLevel(), context.getPlayer(), context.getHand()))
            .orElse(InteractionResult.PASS);
    }
}
