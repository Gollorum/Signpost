package gollorum.signpost.minecraft.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.utils.LangKeys;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static gollorum.signpost.Signpost.MOD_ID;

public class CreativeModeTabRegistry {

    public static void register(IEventBus bus) {
        bus.register(CreativeModeTabRegistry.class);
        Register.register(bus);
    }

    private static final DeferredRegister<CreativeModeTab> Register = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), MOD_ID);
    private static final RegistryObject<CreativeModeTab> _signpostTab = Register.register("signpost", () -> CreativeModeTab.builder()
        .title(Component.translatable(LangKeys.tabGroup))
        .icon(() -> new ItemStack(ItemRegistry.POSTS_ITEMS.get(0).get()))
        .displayItems((params, output) -> {
            output.accept(ItemRegistry.BRUSH.get());
            output.accept(ItemRegistry.WRENCH.get());
            for(var post : ItemRegistry.POSTS_ITEMS)
                output.accept(post.get());
            output.accept(ItemRegistry.WAYSTONE_ITEM.get());
            for(var modelWaystone : ItemRegistry.ModelWaystoneItems)
                if (Config.Server.allowedWaystones.get().contains(modelWaystone._1.name))
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
