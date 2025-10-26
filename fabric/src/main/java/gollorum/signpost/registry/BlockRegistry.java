package gollorum.signpost.registry;

import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.block.WaystoneGeneratorBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import static gollorum.signpost.Signpost.MOD_ID;

public class BlockRegistry {

    public static final WaystoneBlock Waystone = WaystoneBlock.createInstance();
    public static final WaystoneGeneratorBlock WaystoneGenerator = WaystoneGeneratorBlock.getInstance();

    public static void register(){
        Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(MOD_ID, WaystoneBlock.REGISTRY_NAME), Waystone);
        for(var variant : ModelWaystone.variants)
            Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(MOD_ID, variant.registryName), variant.createBlock(ModelWaystone::new));
        for(var variant : PostBlock.AllVariants)
            Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(MOD_ID, variant.registryName), variant.createBlock(PostBlock::new));
        Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(MOD_ID, WaystoneGeneratorBlock.REGISTRY_NAME), WaystoneGenerator);
    }
}