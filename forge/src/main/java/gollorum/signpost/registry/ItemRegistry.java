package gollorum.signpost.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.block.PostItemImpl;
import gollorum.signpost.minecraft.block.*;
import gollorum.signpost.minecraft.items.*;
import gollorum.signpost.utils.Tuple;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Arrays;
import java.util.List;

import static gollorum.signpost.Signpost.MOD_ID;

public class ItemRegistry {

    private static final DeferredRegister<Item> REGISTER = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<Item> WAYSTONE_ITEM =
        REGISTER.register(WaystoneBlock.REGISTRY_NAME,
            () -> new WaystoneItem(WaystoneBlock.getInstance(), new Item.Properties()));

    public static final List<Tuple<ModelWaystone.Variant, RegistryObject<Item>>> ModelWaystoneItems =
        ModelWaystone.variants.stream().map(ItemRegistry::registerModelWaystoneItem).toList();

    public static final List<RegistryObject<Item>> POSTS_ITEMS =
        Arrays.stream(PostBlock.MaterialType.values()).map(ItemRegistry::registerPostItem).toList();

    public static final RegistryObject<Item> WaystoneGeneratorItem =
        REGISTER.register(WaystoneGeneratorBlock.REGISTRY_NAME,
            () -> new BlockItem(BlockRegistry.WaystoneGenerator.get(), new Item.Properties()));

    public static final RegistryObject<Item> WRENCH = REGISTER.register(Wrench.registryName, Wrench::new);

    public static final RegistryObject<Item> BRUSH = REGISTER.register(Brush.registryName, Brush::new);

    public static final RegistryObject<Item> GENERATION_WAND = REGISTER.register(GenerationWand.registryName, GenerationWand::new);

    private static RegistryObject<Item> registerPostItem(PostBlock.MaterialType postVariant){
        return REGISTER.register(
            postVariant.blockRegistryName,
            () -> new PostItemImpl(postVariant.getBlock(), new Item.Properties()));
    }

    private static Tuple<ModelWaystone.Variant, RegistryObject<Item>> registerModelWaystoneItem(ModelWaystone.Variant variant){
        return new Tuple<>(variant, REGISTER.register(
            variant.registryName,
            () -> new WaystoneItem(variant.getBlock(), new Item.Properties())));
    }

    public static void register(IEventBus bus){
        REGISTER.register(bus);
    }
}
