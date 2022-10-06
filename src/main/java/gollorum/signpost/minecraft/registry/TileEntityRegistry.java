package gollorum.signpost.minecraft.registry;

import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.block.tiles.SignGeneratorEntity;
import gollorum.signpost.minecraft.block.tiles.WaystoneGeneratorEntity;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static gollorum.signpost.Signpost.MOD_ID;

public class TileEntityRegistry {

    private static final DeferredRegister<BlockEntityType<?>> REGISTER = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, MOD_ID);

    private static final RegistryObject<BlockEntityType<PostTile>> POST =
        REGISTER.register(PostTile.REGISTRY_NAME, PostTile::createType);

    private static final RegistryObject<BlockEntityType<WaystoneTile>> WAYSTONE =
        REGISTER.register(WaystoneTile.REGISTRY_NAME, WaystoneTile::createType);

    private static final RegistryObject<BlockEntityType<WaystoneGeneratorEntity>> WaystoneGenerator =
        REGISTER.register(WaystoneGeneratorEntity.REGISTRY_NAME, WaystoneGeneratorEntity::createType);

    private static final RegistryObject<BlockEntityType<SignGeneratorEntity>> SignGenerator =
        REGISTER.register(SignGeneratorEntity.REGISTRY_NAME, SignGeneratorEntity::createType);

    public static void register(IEventBus bus){
        REGISTER.register(bus);
    }
}