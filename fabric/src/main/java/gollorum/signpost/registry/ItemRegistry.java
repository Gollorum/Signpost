package gollorum.signpost.registry;

import gollorum.signpost.minecraft.block.*;
import gollorum.signpost.minecraft.items.*;
import gollorum.signpost.utils.Tuple;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

import static gollorum.signpost.Signpost.MOD_ID;

public class ItemRegistry {

    public static final WaystoneItem WAYSTONE_ITEM = new WaystoneItem(
        WaystoneBlock.getInstance(),
        new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, WaystoneBlock.REGISTRY_NAME)))
    );

    public static final List<Tuple<ModelWaystone.Variant, Item>> ModelWaystoneItems = new ArrayList<>();

    public static final List<Item> POSTS_ITEMS = new ArrayList<>();

    /** See {@link PostBlock.LegacyVariant} - registered only so that saves written before 2.04 still load. */
    public static final List<Item> LEGACY_POSTS_ITEMS = new ArrayList<>();

    public static final Item WaystoneGeneratorItem = new BlockItem(
        BlockRegistry.WaystoneGenerator,
        new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, WaystoneGeneratorBlock.REGISTRY_NAME)))
    );

    public static final Item WRENCH = new Wrench();

    public static final Item BRUSH = new Brush();

    public static final Item GENERATION_WAND = new GenerationWand();

    public static void register(){
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, WaystoneBlock.REGISTRY_NAME), WAYSTONE_ITEM);
        for(var variant : ModelWaystone.variants)
            ModelWaystoneItems.add(Tuple.of(
                variant,
                Registry.register(
                    BuiltInRegistries.ITEM,
                    Identifier.fromNamespaceAndPath(MOD_ID, variant.registryName),
                    new WaystoneItem(
                        variant.getBlock(),
                        new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, variant.registryName)))))
            ));
        for(var materialType : PostBlock.MaterialType.values())
            POSTS_ITEMS.add(Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(MOD_ID, materialType.blockRegistryName),
                new PostItem(materialType.getBlock(), new Item.Properties())
            ));
        for(var variant : PostBlock.LegacyVariants)
            LEGACY_POSTS_ITEMS.add(Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(MOD_ID, variant.registryName),
                new PostItem(variant.getBlock(), new Item.Properties())
            ));
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, WaystoneGeneratorBlock.REGISTRY_NAME), WaystoneGeneratorItem);
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, Wrench.registryName), WRENCH);
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, Brush.registryName), BRUSH);
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, GenerationWand.registryName), GENERATION_WAND);
    }
}
