package gollorum.signpost.minecraft.registry;

import gollorum.signpost.minecraft.block.*;
import gollorum.signpost.minecraft.items.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

import static gollorum.signpost.Signpost.MOD_ID;

public class ItemRegistry {

    public static final CreativeModeTab ITEM_GROUP = new CreativeModeTab("signpost") {
        @Override
        public @NotNull ItemStack makeIcon() {
            return new ItemStack(POSTS_ITEMS.get(0).get());
        }
    };

    private static final DeferredRegister<Item> REGISTER = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    private static final RegistryObject<Item> WAYSTONE_ITEM =
        REGISTER.register(WaystoneBlock.REGISTRY_NAME,
            () -> new WaystoneItem(WaystoneBlock.getInstance(), new Item.Properties().tab(ITEM_GROUP)));

    private static final List<RegistryObject<Item>> ModelWaystoneItems =
        ModelWaystone.variants.stream().map(ItemRegistry::registerModelWaystoneItem).toList();

    private static final List<RegistryObject<Item>> POSTS_ITEMS =
        PostBlock.AllVariants.stream().map(ItemRegistry::registerPostItem).toList();

    private static final RegistryObject<Item> WaystoneGeneratorItem =
        REGISTER.register(WaystoneGeneratorBlock.REGISTRY_NAME,
            () -> new BlockItem(BlockRegistry.WaystoneGenerator.get(), new Item.Properties()));

    public static final RegistryObject<Item> WRENCH = REGISTER.register(Wrench.registryName, () -> new Wrench(ITEM_GROUP));

    public static final RegistryObject<Item> BRUSH = REGISTER.register(Brush.registryName, () -> new Brush(ITEM_GROUP));

    public static final RegistryObject<Item> GENERATION_WAND = REGISTER.register(GenerationWand.registryName, GenerationWand::new);

    private static RegistryObject<Item> registerPostItem(PostBlock.Variant postVariant){
        return REGISTER.register(
            postVariant.registryName,
            () -> new PostItem(postVariant.getBlock(), new Item.Properties().tab(ITEM_GROUP)));
    }

    private static RegistryObject<Item> registerModelWaystoneItem(ModelWaystone.Variant variant){
        return REGISTER.register(
            variant.registryName,
            () -> new WaystoneItem(variant.getBlock(), new Item.Properties().tab(ITEM_GROUP)));
    }

    public static void register(IEventBus bus){
        REGISTER.register(bus);
    }
}
