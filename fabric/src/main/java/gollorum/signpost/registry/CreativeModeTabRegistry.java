package gollorum.signpost.registry;

import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.minecraft.utils.LangKeys;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import static gollorum.signpost.Signpost.MOD_ID;

public class CreativeModeTabRegistry {

    private static final CreativeModeTab _signpostTab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
        .title(Component.translatable(LangKeys.tabGroup))
        .icon(() -> new ItemStack(ItemRegistry.POSTS_ITEMS.get(0)))
        .displayItems((params, output) -> {
            output.accept(ItemRegistry.BRUSH);
            output.accept(ItemRegistry.WRENCH);
            for(var post : ItemRegistry.POSTS_ITEMS)
                output.accept(post);
            output.accept(ItemRegistry.WAYSTONE_ITEM);
            for(var modelWaystone : ItemRegistry.ModelWaystoneItems)
                if (IConfig.IServer.getInstance().allowedWaystones().contains(modelWaystone._1().name))
                    output.accept(modelWaystone._2());
        })
        .build();

    public static void register() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(MOD_ID, "signpost"), _signpostTab);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.OP_BLOCKS).register(CreativeModeTabRegistry::onBuildCreativeModeContents);
    }

    public static void onBuildCreativeModeContents(FabricItemGroupEntries entries) {
        entries.accept(ItemRegistry.WaystoneGeneratorItem);
        entries.accept(ItemRegistry.GENERATION_WAND);
    }
}