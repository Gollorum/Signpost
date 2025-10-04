package gollorum.signpost.registry;

import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.block.WaystoneGeneratorBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

import static gollorum.signpost.Signpost.MOD_ID;

public class BlockRegistry {

    private static final DeferredRegister.Blocks Register = DeferredRegister.createBlocks(MOD_ID);

    public static final DeferredBlock<WaystoneBlock> WaystoneBlock = Register.register(
        gollorum.signpost.minecraft.block.WaystoneBlock.REGISTRY_NAME,
        gollorum.signpost.minecraft.block.WaystoneBlock::createInstance);

    public static final List<DeferredBlock<ModelWaystone>> ModelWaystoneBlocks =
        ModelWaystone.variants.stream().map(BlockRegistry::registerModelWaystone).toList();

    public static final List<DeferredBlock<PostBlock>> PostBlocks =
        PostBlock.AllVariants.stream().map(BlockRegistry::registerPostBlock).toList();

    private static DeferredBlock<PostBlock> registerPostBlock(PostBlock.Variant variant) {
        return Register.register(variant.registryName, () -> variant.createBlock(PostBlock::new));
    }

    private static DeferredBlock<ModelWaystone> registerModelWaystone(ModelWaystone.Variant variant) {
        return Register.register(variant.registryName, () -> variant.createBlock(ModelWaystone::new));
    }

    public static final DeferredBlock<WaystoneGeneratorBlock> WaystoneGenerator =
        Register.register(WaystoneGeneratorBlock.REGISTRY_NAME, WaystoneGeneratorBlock::getInstance);

    public static void register(IEventBus bus){
        Register.register(bus);
    }
}