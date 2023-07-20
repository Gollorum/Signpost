package gollorum.signpost.minecraft.items;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.BlockEventListener;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.registry.BlockRegistry;
import gollorum.signpost.minecraft.registry.ItemRegistry;
import gollorum.signpost.minecraft.utils.LangKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CreativeModeTabEventListeners {

    public static void register(IEventBus bus) { bus.register(CreativeModeTabEventListeners.class); }

    private static CreativeModeTab _signpostTab;

    @SubscribeEvent
    public static void onRegisterTab(CreativeModeTabEvent.Register event) {
        _signpostTab = event.registerCreativeModeTab(
            new ResourceLocation(Signpost.MOD_ID, "signpost"),
            b -> b
                .icon(()-> new ItemStack(ItemRegistry.POSTS_ITEMS.get(0).get()))
                .title(Component.translatable(LangKeys.tabGroup))
        );
    }

    @SubscribeEvent
    public static void onBuildCreativeModeContents(CreativeModeTabEvent.BuildContents event) {
        if (event.getTab() == _signpostTab) {
            event.accept(ItemRegistry.BRUSH);
            event.accept(ItemRegistry.WRENCH);
            for(var post : ItemRegistry.POSTS_ITEMS)
                event.accept(post);
            event.accept(ItemRegistry.WAYSTONE_ITEM);
            for(var modelWaystone : ItemRegistry.ModelWaystoneItems)
                if (Config.Server.allowedWaystones.get().contains(modelWaystone._1.name))
                    event.accept(modelWaystone._2);
        }
        if(event.getTab() == CreativeModeTabs.OP_BLOCKS) {
            event.accept(ItemRegistry.WaystoneGeneratorItem);
            event.accept(ItemRegistry.GENERATION_WAND);
        }
    }
}
