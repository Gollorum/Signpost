package gollorum.signpost.utils;

import gollorum.signpost.Signpost;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import java.util.Optional;

public class TileEntityUtils {

    public static <T extends TileEntity> Optional<T> findTileEntity(IWorld world, BlockPos pos, Class<T> c){
        TileEntity tileEntity = world.getTileEntity(pos);
        if(tileEntity != null && tileEntity.getClass() == c){
            return Optional.of((T) tileEntity);
        } else return Optional.empty();
    }

    public static <T extends TileEntity> Optional<T> findTileEntity(ResourceLocation dimensionKeyLocation, boolean isRemote, BlockPos blockPos, Class<T> c){
        return findWorld(dimensionKeyLocation, isRemote).flatMap(world -> findTileEntity(world, blockPos, c));
    }

    public static Optional<World> findWorld(ResourceLocation dimensionKeyLocation, boolean isRemote) {
        return isRemote
            ? (Minecraft.getInstance().world.getDimensionKey().getLocation().equals(dimensionKeyLocation)
                ? Optional.of(Minecraft.getInstance().world)
                : Optional.empty())
            : (Signpost.getServerType().isServer
                ? Optional.ofNullable(Signpost.getServerInstance()
                    .getWorld(RegistryKey.getOrCreateKey(Registry.WORLD_KEY, dimensionKeyLocation)))
                : Optional.empty());
    }

    public static <T extends TileEntity> Optional<T> findTileEntityClient(ResourceLocation dimensionKeyLocation, BlockPos pos, Class<T> c){
        return Minecraft.getInstance().world.getDimensionKey().getLocation().equals(dimensionKeyLocation)
            ? findTileEntity(Minecraft.getInstance().world, pos, c)
            : Optional.empty();
    }

}
