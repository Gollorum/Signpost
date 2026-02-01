package gollorum.signpost.registry;

import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.block.tiles.WaystoneGeneratorEntity;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

import static gollorum.signpost.Signpost.MOD_ID;

public class TileEntityRegistry {

    private static final BlockEntityType<PostTile> POST = PostTile.createType();

    private static final BlockEntityType<WaystoneTile> WAYSTONE = WaystoneTile.createType();

    private static final BlockEntityType<WaystoneGeneratorEntity> WaystoneGenerator = WaystoneGeneratorEntity.createType();

    public static void register(){
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, PostTile.REGISTRY_NAME), POST);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, WaystoneTile.REGISTRY_NAME), WAYSTONE);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, WaystoneGeneratorEntity.REGISTRY_NAME), WaystoneGenerator);
    }
}