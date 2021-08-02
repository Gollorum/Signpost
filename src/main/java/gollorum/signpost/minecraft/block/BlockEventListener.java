package gollorum.signpost.minecraft.block;

import gollorum.signpost.BlockRestrictions;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.security.WithCountRestriction;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class BlockEventListener {

    public static void register(IEventBus bus) { bus.register(BlockEventListener.class); }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if(!event.isCanceled() && event.getPlacedBlock().getBlock() instanceof WithCountRestriction) {
            BlockRestrictions.Type restrictionType = ((WithCountRestriction)event.getPlacedBlock().getBlock()).getBlockRestrictionType();
            PlayerHandle player = PlayerHandle.from(event.getEntity());
            if(!BlockRestrictions.getInstance().tryDecrementRemaining(restrictionType, player)) {
                event.setCanceled(true);
                event.getEntity().sendMessage(new TranslationTextComponent(restrictionType.errorLangKey), Util.DUMMY_UUID);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockRemoved(BlockEvent.BreakEvent event) {
        Block block = event.getState().getBlock();
        TileEntity tile = event.getWorld().getTileEntity(event.getPos());
        if(!event.isCanceled() && block instanceof WithCountRestriction) {
            BlockRestrictions.Type restrictionType = ((WithCountRestriction)event.getState().getBlock()).getBlockRestrictionType();
            restrictionType.tryGetOwner.apply(tile).ifPresent(owner ->
                BlockRestrictions.getInstance().incrementRemaining(restrictionType, owner));
        }
    }

}
