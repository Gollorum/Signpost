package gollorum.signpost.minecraft.registry;

import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.Post;
import gollorum.signpost.minecraft.block.Waystone;
import net.minecraft.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;

import static gollorum.signpost.Signpost.MOD_ID;

public class BlockRegistry {

    private static final DeferredRegister<Block> Register = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);

    private static final RegistryObject<Block> WaystoneBlock =
        Register.register(Waystone.REGISTRY_NAME, () -> Waystone.INSTANCE);

    private static final Object[] ModelWaystoneBlocks =
        ModelWaystone.variants.stream()
            .map(BlockRegistry::registerModelWaystone)
            .toArray();

    private static final Object[] PostBlocks =
        Arrays.stream(Post.AllVariants)
            .map(BlockRegistry::registerPostBlock)
            .toArray();

    private static RegistryObject<Block> registerPostBlock(Post.Variant postVariant) {
        return Register.register(postVariant.registryName, () -> postVariant.block);
    }

    private static RegistryObject<Block> registerModelWaystone(ModelWaystone.Variant variant) {
        return Register.register(variant.registryName, () -> variant.block);
    }

    public static void register(IEventBus bus){
        Register.register(bus);
    }
}