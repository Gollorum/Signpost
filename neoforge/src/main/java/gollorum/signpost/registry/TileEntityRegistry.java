package gollorum.signpost.registry;

import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.block.tiles.WaystoneGeneratorEntity;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static gollorum.signpost.Signpost.MOD_ID;

public class TileEntityRegistry {

    private static final DeferredRegister<BlockEntityType<?>> REGISTER = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);

    private static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PostTile>> POST =
        REGISTER.register(PostTile.REGISTRY_NAME, PostTile::createType);

    private static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaystoneTile>> WAYSTONE =
        REGISTER.register(WaystoneTile.REGISTRY_NAME, WaystoneTile::createType);

    private static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaystoneGeneratorEntity>> WaystoneGenerator =
        REGISTER.register(WaystoneGeneratorEntity.REGISTRY_NAME, WaystoneGeneratorEntity::createType);

    public static void register(IEventBus bus){
        REGISTER.register(bus);
    }
}