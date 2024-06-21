package gollorum.signpost.registry;

import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.minecraft.utils.LangKeys;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static gollorum.signpost.Signpost.MOD_ID;

public class CreativeModeTabRegistry {

    public static void register(IEventBus bus) {
        bus.register(CreativeModeTabRegistry.class);
        Register.register(bus);
    }

    private static final DeferredRegister<CreativeModeTab> Register = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), MOD_ID);
    private static final DeferredHolder<CreativeModeTab, CreativeModeTab> _signpostTab = Register.register("signpost", () -> CreativeModeTab.builder()
        .title(Component.translatable(LangKeys.tabGroup))
        .icon(() -> new ItemStack(ItemRegistry.POSTS_ITEMS.get(0).get()))
        .displayItems((params, output) -> {
            output.accept(ItemRegistry.BRUSH.get());
            output.accept(ItemRegistry.WRENCH.get());
            for(var post : ItemRegistry.POSTS_ITEMS)
                output.accept(post.get());
            output.accept(ItemRegistry.WAYSTONE_ITEM.get());
            for(var modelWaystone : ItemRegistry.ModelWaystoneItems)
                if (IConfig.IServer.getInstance().allowedWaystones().contains(modelWaystone._1.name))
                    output.accept(modelWaystone._2.get());
        })
        .build()
    );

    @SubscribeEvent
    public static void onBuildCreativeModeContents(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey() == CreativeModeTabs.OP_BLOCKS) {
            event.accept(ItemRegistry.WaystoneGeneratorItem);
            event.accept(ItemRegistry.GENERATION_WAND);
        }
    }
}
