package gollorum.signpost.minecraft.registry;

import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.stream.Collectors;

import static gollorum.signpost.Signpost.MOD_ID;

public class BlockRegistry {

    private static final DeferredRegister<Block> Register = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);

    private static final RegistryObject<Block> WaystoneBlock =
        Register.register(gollorum.signpost.minecraft.block.WaystoneBlock.REGISTRY_NAME, () -> gollorum.signpost.minecraft.block.WaystoneBlock.INSTANCE);

    private static final List<RegistryObject<Block>> ModelWaystoneBlocks =
        ModelWaystone.variants.stream()
            .map(BlockRegistry::registerModelWaystone)
            .collect(Collectors.toList());

    private static final List<RegistryObject<Block>> PostBlocks =
        PostBlock.AllVariants.stream()
            .map(BlockRegistry::registerPostBlock)
            .collect(Collectors.toList());

    private static RegistryObject<Block> registerPostBlock(PostBlock.Variant postVariant) {
        return Register.register(postVariant.registryName, () -> postVariant.block);
    }

    private static RegistryObject<Block> registerModelWaystone(ModelWaystone.Variant variant) {
        return Register.register(variant.registryName, () -> variant.block);
    }

    public static void register(IEventBus bus){
        Register.register(bus);
    }
}