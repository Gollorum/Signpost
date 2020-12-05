package gollorum.signpost.minecraft.registry;

import gollorum.signpost.minecraft.block.Post;
import gollorum.signpost.minecraft.block.Waystone;
import net.minecraft.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;

import static gollorum.signpost.Signpost.MOD_ID;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class BlockRegistry {

    private static final DeferredRegister<Block> REGISTER = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);

    private static final RegistryObject<Block> WAYSTONE_BLOCK =
        REGISTER.register(Waystone.REGISTRY_NAME, () -> Waystone.INSTANCE);

    private static final Object[] POST_BLOCKS =
        Arrays.stream(Post.All_INFOS)
            .map(BlockRegistry::registerPostBlock)
            .toArray();

    private static RegistryObject<Block> registerPostBlock(Post.Info postInfo){
        return REGISTER.register(postInfo.registryName, () -> postInfo.post);
    }

    public static void register(IEventBus bus){
        REGISTER.register(bus);
    }
}