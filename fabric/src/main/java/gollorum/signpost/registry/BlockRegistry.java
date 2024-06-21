package gollorum.signpost.registry;

import gollorum.signpost.block.ModelWaystoneImpl;
import gollorum.signpost.block.PostBlockImpl;
import gollorum.signpost.block.WaystoneBlockImpl;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.block.WaystoneGeneratorBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import static gollorum.signpost.Signpost.MOD_ID;

public class BlockRegistry {

    public static final WaystoneBlock Waystone = WaystoneBlockImpl.createInstance();
    public static final WaystoneGeneratorBlock WaystoneGenerator = WaystoneGeneratorBlock.getInstance();

    public static void register(){
        Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(MOD_ID, WaystoneBlock.REGISTRY_NAME), Waystone);
        for(var variant : ModelWaystone.variants)
            Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(MOD_ID, variant.registryName), variant.createBlock(ModelWaystoneImpl::new));
        for(var variant : PostBlock.AllVariants)
            Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(MOD_ID, variant.registryName), variant.createBlock(PostBlockImpl::new));
        Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(MOD_ID, WaystoneGeneratorBlock.REGISTRY_NAME), WaystoneGenerator);
    }
}