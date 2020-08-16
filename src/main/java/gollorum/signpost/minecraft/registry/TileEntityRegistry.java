package gollorum.signpost.minecraft.registry;

import gollorum.signpost.minecraft.block.tiles.PostTile;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import static gollorum.signpost.Signpost.MOD_ID;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class TileEntityRegistry {

    private static final DeferredRegister<TileEntityType<?>> REGISTER = new DeferredRegister<>(ForgeRegistries.TILE_ENTITIES, MOD_ID);

    private static final RegistryObject<TileEntityType<PostTile>> POST =
        REGISTER.register(PostTile.REGISTRY_NAME, () -> PostTile.type);

    public static void register(IEventBus bus){
        REGISTER.register(bus);
    }
}