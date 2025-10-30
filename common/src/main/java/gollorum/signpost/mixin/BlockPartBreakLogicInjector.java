package gollorum.signpost.mixin;

import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.IDelay;
import gollorum.signpost.utils.WorldLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Optional;

@Mixin(ServerPlayerGameMode.class)
public abstract class BlockPartBreakLogicInjector {

    @Accessor
    protected abstract ServerLevel getLevel();
    @Accessor
    protected abstract ServerPlayer getPlayer();

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void onBeforeDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        var level = getLevel();
        BlockEntity tile = level.getBlockEntity(pos);
        if(tile instanceof PostTile postTile) {
            Optional<PostTile.TraceResult> traceResult = postTile.trace(getPlayer());
            if(traceResult.isPresent() && !(traceResult.get().part.blockPart() instanceof PostBlockPart)) {
                IDelay.onServerForFrames(1, () -> {

                    // The client destroys the tile entity instantly. When the event gets cancelled, the server
                    // sends a block update to the client to restore the block. For some reason, it can happen
                    // that the entity update packet arrives **before** the entity has been reconstructed, which
                    // leaves an empty, and thus invisible, post. To fix that, we manually send another update
                    // one frame later.
                    PacketHandler.getInstance().sendToTracing(tile, () -> PostTile.UpdateAllPartsEvent.Packet.from(tile.getUpdateTag(level.registryAccess()), WorldLocation.from(tile).get()));

                    postTile.removePart(traceResult.get().id);
                    if (level instanceof ServerLevel) {
                        if (!getPlayer().isCreative()) {
                            for (ItemStack item : (Collection<ItemStack>) traceResult.get().part.blockPart().getDrops()) {
                                ItemEntity itementity = new ItemEntity(
                                    level,
                                    pos.getX() + level.getRandom().nextFloat() * 0.5 + 0.25,
                                    pos.getY() + level.getRandom().nextFloat() * 0.5 + 0.25,
                                    pos.getZ() + level.getRandom().nextFloat() * 0.5 + 0.25,
                                    item
                                );
                                itementity.setDefaultPickUpDelay();
                                level.addFreshEntity(itementity);
                            }
                        }
                    }
                });
                
                cir.setReturnValue(false);
            } else postTile.onDestroy();
        }
    }

}
