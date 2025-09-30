package gollorum.signpost.registry;

import gollorum.signpost.block.PostItemImpl;
import gollorum.signpost.minecraft.block.*;
import gollorum.signpost.minecraft.items.*;
import gollorum.signpost.utils.Tuple;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

import static gollorum.signpost.Signpost.MOD_ID;

public class ItemRegistry {

    private static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(MOD_ID);

    public static final DeferredItem<WaystoneItem> WAYSTONE_ITEM =
        REGISTER.register(WaystoneBlock.REGISTRY_NAME,
            () -> new WaystoneItem(
                WaystoneBlock.getInstance(),
                new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, WaystoneBlock.REGISTRY_NAME)))));

    public static final List<Tuple<ModelWaystone.Variant, DeferredItem<Item>>> ModelWaystoneItems =
        ModelWaystone.variants.stream().map(ItemRegistry::registerModelWaystoneItem).toList();

    public static final List<DeferredItem<Item>> POSTS_ITEMS =
        PostBlock.AllVariants.stream().map(ItemRegistry::registerPostItem).toList();

    public static final DeferredItem<Item> WaystoneGeneratorItem =
        REGISTER.register(WaystoneGeneratorBlock.REGISTRY_NAME,
            () -> new BlockItem(
                BlockRegistry.WaystoneGenerator.get(),
                new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, WaystoneGeneratorBlock.REGISTRY_NAME)))
            ));

    public static final DeferredItem<Item> WRENCH = REGISTER.register(Wrench.registryName, Wrench::new);

    public static final DeferredItem<Item> BRUSH = REGISTER.register(Brush.registryName, Brush::new);

    public static final DeferredItem<Item> GENERATION_WAND = REGISTER.register(GenerationWand.registryName, GenerationWand::new);

    private static DeferredItem<Item> registerPostItem(PostBlock.Variant postVariant) {
        return REGISTER.register(
            postVariant.registryName,
            () -> new PostItemImpl(
                postVariant.getBlock(),
                new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, postVariant.registryName)))
            ));
    }

    private static Tuple<ModelWaystone.Variant, DeferredItem<Item>> registerModelWaystoneItem(ModelWaystone.Variant variant){
        return new Tuple<>(variant, REGISTER.register(
            variant.registryName,
            () -> new WaystoneItem(
                variant.getBlock(),
                new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, variant.registryName))))));
    }

    public static void register(IEventBus bus){
        REGISTER.register(bus);
    }
}
