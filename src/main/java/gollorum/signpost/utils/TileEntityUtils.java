package gollorum.signpost.utils;

import gollorum.signpost.Signpost;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.DimensionManager;

import java.util.Optional;

public class TileEntityUtils {

    public static <T extends TileEntity> Optional<T> findTileEntity(World world, BlockPos pos, Class<T> c){
        TileEntity tileEntity = world.getTileEntity(pos);
        if(tileEntity != null && tileEntity.getClass() == c){
            return Optional.of((T) tileEntity);
        } else return Optional.empty();
    }

    public static <T extends TileEntity> Optional<T> findTileEntity(int dimension, boolean isRemote, BlockPos blockPos, Class<T> c){
        return findWorld(dimension, isRemote).flatMap(world -> findTileEntity(world, blockPos, c));
    }

    public static Optional<World> findWorld(int dimension, boolean isRemote) {
        DimensionType dimensionType = DimensionType.getById(dimension);
        return isRemote
            ? (Minecraft.getInstance().world.getDimension().getType().equals(dimensionType)
                ? Optional.of(Minecraft.getInstance().world)
                : Optional.empty())
            : (Signpost.getServerType().isServer && dimensionType != null
                ? Optional.ofNullable(DimensionManager.getWorld(Signpost.getServerInstance(), dimensionType, true, false))
                : Optional.empty());
    }

    public static <T extends TileEntity> Optional<T> findTileEntityClient(DimensionType dimension, BlockPos pos, Class<T> c){
        return Minecraft.getInstance().world.getDimension().getType().equals(dimension)
            ? findTileEntity(Minecraft.getInstance().world, pos, c)
            : Optional.empty();
    }

}
