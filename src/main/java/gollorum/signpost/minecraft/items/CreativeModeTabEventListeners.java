package gollorum.signpost.minecraft.items;

import gollorum.signpost.minecraft.block.BlockEventListener;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.config.Config;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CreativeModeTabEventListeners {

    public static void register(IEventBus bus) { bus.register(BlockEventListener.class); }

    @SubscribeEvent
    public static void onBuildCreativeModeContents(CreativeModeTabEvent.BuildContents event) {
        for(var element : event.getEntries()) {
            if(element.getKey().getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ModelWaystone modelWaystone
                && !Config.Server.allowedWaystones.get().contains(modelWaystone.variant.name))
                event.getEntries().remove(element.getKey());
        }
    }
}
