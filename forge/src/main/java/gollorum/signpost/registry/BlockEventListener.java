package gollorum.signpost.registry;

import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.IDelay;
import gollorum.signpost.utils.WorldLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collection;
import java.util.Optional;

public class BlockEventListener {

    public static void register(IEventBus bus) { bus.register(BlockEventListener.class); }

    @SubscribeEvent
    public static void onBlockRemoved(BlockEvent.BreakEvent event) {
        BlockEntity tile = event.getLevel().getBlockEntity(event.getPos());
        if(!event.isCanceled() && tile instanceof PostTile) {
            PostTile postTile = (PostTile) tile;
            Optional<PostTile.TraceResult> traceResult = postTile.trace(event.getPlayer());
            if(traceResult.isPresent() && !(traceResult.get().part.blockPart() instanceof PostBlockPart)) {
                event.setCanceled(true);
                IDelay.onServerForFrames(1, () -> {

                    // The client destroys the tile entity instantly. When the event gets cancelled, the server
                    // sends a block update to the client to restore the block. For some reason, it can happen
                    // that the entity update packet arrives **before** the entity has been reconstructed, which
                    // leaves an empty, and thus invisible, post. To fix that, we manually send another update
                    // one frame later.
                    PacketHandler.getInstance().sendToTracing(tile, () -> new PostTile.UpdateAllPartsEvent.Packet(tile.getUpdateTag(event.getLevel().registryAccess()), WorldLocation.from(tile).get()));

                    postTile.removePart(traceResult.get().id);
                    if (event.getLevel() instanceof ServerLevel) {
                        ServerLevel world = (ServerLevel) event.getLevel();
                        if (!event.getPlayer().isCreative()) {
                            BlockPos pos = tile.getBlockPos();
                            for (ItemStack item : (Collection<ItemStack>) traceResult.get().part.blockPart().getDrops()) {
                                ItemEntity itementity = new ItemEntity(
                                    world,
                                    pos.getX() + world.getRandom().nextFloat() * 0.5 + 0.25,
                                    pos.getY() + world.getRandom().nextFloat() * 0.5 + 0.25,
                                    pos.getZ() + world.getRandom().nextFloat() * 0.5 + 0.25,
                                    item
                                );
                                itementity.setDefaultPickUpDelay();
                                world.addFreshEntity(itementity);
                            }
                        }
                    }
                });
            } else postTile.onDestroy();
        }
    }

}
