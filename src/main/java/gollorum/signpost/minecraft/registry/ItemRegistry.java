package gollorum.signpost.minecraft.registry;

import gollorum.signpost.minecraft.Wrench;
import gollorum.signpost.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static gollorum.signpost.Signpost.MOD_ID;

public class ItemRegistry {

    public static final ItemGroup ITEM_GROUP = new ItemGroup("signpost") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(POSTS_ITEMS.get(0).get());
        }
    };

    private static final DeferredRegister<Item> REGISTER = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    private static final RegistryObject<Item> WAYSTONE_ITEM =
        REGISTER.register(Waystone.REGISTRY_NAME,
            () -> new BlockItem(Waystone.INSTANCE, new Item.Properties().group(ITEM_GROUP)));

    private static final RegistryObject<Item> GENERATED_WAYSTONE_ITEM =
        REGISTER.register(VillageWaystone.REGISTRY_NAME,
            () -> new BlockItem(VillageWaystone.INSTANCE, new Item.Properties()));

    private static final RegistryObject<Item> GENERATED_POST_ITEM =
        REGISTER.register(VillagePost.REGISTRY_NAME,
            () -> new BlockItem(VillagePost.INSTANCE, new Item.Properties()));

    private static final List<RegistryObject<Item>> ModelWaystoneItems =
        ModelWaystone.variants.stream()
            .map(ItemRegistry::registerModelWaystoneItem)
            .collect(Collectors.toList());

    private static final List<RegistryObject<Item>> POSTS_ITEMS =
        Arrays.stream(Post.AllVariants)
            .map(ItemRegistry::registerPostItem)
            .collect(Collectors.toList());

    public static final RegistryObject<Item> WRENCH = REGISTER.register(Wrench.registryName, () -> new Wrench(ITEM_GROUP));

    private static RegistryObject<Item> registerPostItem(Post.Variant postVariant){
        return REGISTER.register(
            postVariant.registryName,
            () -> new BlockItem(postVariant.block, new Item.Properties().group(ITEM_GROUP)));
    }

    private static RegistryObject<Item> registerModelWaystoneItem(ModelWaystone.Variant variant){
        return REGISTER.register(
            variant.registryName,
            () -> new BlockItem(variant.block, new Item.Properties().group(ITEM_GROUP)));
    }

    public static void register(IEventBus bus){
        REGISTER.register(bus);
    }
}
