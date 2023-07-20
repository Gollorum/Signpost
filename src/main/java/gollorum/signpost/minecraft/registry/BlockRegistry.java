package gollorum.signpost.minecraft.registry;

import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneGeneratorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

import static gollorum.signpost.Signpost.MOD_ID;

public class BlockRegistry {

    private static final DeferredRegister<Block> Register = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);

    public static final RegistryObject<Block> WaystoneBlock =
        Register.register(gollorum.signpost.minecraft.block.WaystoneBlock.REGISTRY_NAME,
            gollorum.signpost.minecraft.block.WaystoneBlock::createInstance);

    public static final List<RegistryObject<ModelWaystone>> ModelWaystoneBlocks =
        ModelWaystone.variants.stream().map(BlockRegistry::registerModelWaystone).toList();

    public static final List<RegistryObject<PostBlock>> PostBlocks =
        PostBlock.AllVariants.stream().map(BlockRegistry::registerPostBlock).toList();

    private static RegistryObject<PostBlock> registerPostBlock(PostBlock.Variant variant) {
        return Register.register(variant.registryName, variant::createBlock);
    }

    private static RegistryObject<ModelWaystone> registerModelWaystone(ModelWaystone.Variant variant) {
        return Register.register(variant.registryName, variant::createBlock);
    }

    public static final RegistryObject<WaystoneGeneratorBlock> WaystoneGenerator =
        Register.register(WaystoneGeneratorBlock.REGISTRY_NAME, WaystoneGeneratorBlock::new);

    public static void register(IEventBus bus){
        Register.register(bus);
    }
}