package gollorum.signpost.registry;

import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.block.WaystoneGeneratorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import static gollorum.signpost.Signpost.MOD_ID;

public class BlockRegistry {

    public static final WaystoneBlock Waystone = WaystoneBlock.createInstance();
    public static final WaystoneGeneratorBlock WaystoneGenerator = WaystoneGeneratorBlock.getInstance();

    public static void register(){
        Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, WaystoneBlock.REGISTRY_NAME), Waystone);
        for(var variant : ModelWaystone.variants)
            Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, variant.registryName), variant.createBlock(ModelWaystone::new));
        for(var materialType : PostBlock.MaterialType.values())
            Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, materialType.blockRegistryName), materialType.createBlock());
        // See PostBlock.LegacyVariant - registered only so that saves written before 2.04 still load.
        for(var variant : PostBlock.LegacyVariants)
            Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, variant.registryName), variant.createBlock());
        Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, WaystoneGeneratorBlock.REGISTRY_NAME), WaystoneGenerator);
    }
}