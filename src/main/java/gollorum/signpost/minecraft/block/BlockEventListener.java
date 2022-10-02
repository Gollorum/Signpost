package gollorum.signpost.minecraft.block;

import gollorum.signpost.BlockRestrictions;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.security.WithCountRestriction;
import gollorum.signpost.utils.Delay;
import gollorum.signpost.utils.WorldLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collection;
import java.util.Optional;

public class BlockEventListener {

    public static void register(IEventBus bus) { bus.register(BlockEventListener.class); }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if(!event.isCanceled() && event.getPlacedBlock().getBlock() instanceof WithCountRestriction) {
            BlockRestrictions.Type restrictionType = ((WithCountRestriction)event.getPlacedBlock().getBlock()).getBlockRestrictionType();
            PlayerHandle player = PlayerHandle.from(event.getEntity());
            if(!BlockRestrictions.getInstance().tryDecrementRemaining(restrictionType, player))
                event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockRemoved(BlockEvent.BreakEvent event) {
        Block block = event.getState().getBlock();
        BlockEntity tile = event.getLevel().getBlockEntity(event.getPos());
        if(!event.isCanceled() && tile instanceof PostTile) {
            PostTile postTile = (PostTile) tile;
            Optional<PostTile.TraceResult> traceResult = postTile.trace(event.getPlayer());
            if(traceResult.isPresent() && !(traceResult.get().part.blockPart instanceof PostBlockPart)) {
                event.setCanceled(true);
                Delay.onServerForFrames(1, () -> {

                    // The client destroys the tile entity instantly. When the event gets cancelled, the server
                    // sends a block update to the client to restore the block. For some reason, it can happen
                    // that the entity update packet arrives **before** the entity has been reconstructed, which
                    // leaves an empty, and thus invisible, post. To fix that, we manually send another update
                    // one frame later.
                    PacketHandler.sendToTracing(tile, () -> new PostTile.UpdateAllPartsEvent.Packet(tile.getUpdateTag(), WorldLocation.from(tile).get()));

                    postTile.removePart(traceResult.get().id);
                    if (event.getLevel() instanceof ServerLevel) {
                        ServerLevel world = (ServerLevel) event.getLevel();
                        if (!event.getPlayer().isCreative()) {
                            BlockPos pos = tile.getBlockPos();
                            for (ItemStack item : (Collection<ItemStack>) traceResult.get().part.blockPart.getDrops(postTile)) {
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
        if(!event.isCanceled() && block instanceof WithCountRestriction) {
            BlockRestrictions.Type restrictionType = ((WithCountRestriction)block).getBlockRestrictionType();
            restrictionType.tryGetOwner.apply(tile).ifPresent(owner ->
                BlockRestrictions.getInstance().incrementRemaining(restrictionType, owner));

            if(event.getLevel() instanceof ServerLevel) {
                WaystoneTile.onRemoved((ServerLevel) event.getLevel(), event.getPos());
            }
        }
    }

}
