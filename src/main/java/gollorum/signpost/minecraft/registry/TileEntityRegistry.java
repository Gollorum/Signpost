package gollorum.signpost.minecraft.registry;

import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import static gollorum.signpost.Signpost.MOD_ID;

public class TileEntityRegistry {

    private static final DeferredRegister<TileEntityType<?>> REGISTER = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, MOD_ID);

    private static final RegistryObject<TileEntityType<PostTile>> POST =
        REGISTER.register(PostTile.REGISTRY_NAME, () -> PostTile.type);

    private static final RegistryObject<TileEntityType<WaystoneTile>> WAYSTONE =
        REGISTER.register(WaystoneTile.REGISTRY_NAME, () -> WaystoneTile.type);

    public static void register(IEventBus bus){
        REGISTER.register(bus);
    }
}