package gollorum.signpost.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.Optional;

public class TileEntityUtils {

    public static <T extends TileEntity> Optional<T> findTileEntity(World world, BlockPos pos, Class<T> c){
        TileEntity tileEntity = world.getTileEntity(pos);
        if(tileEntity != null && tileEntity.getClass() == c){
            return Optional.of((T) tileEntity);
        } else return Optional.empty();
    }

    public static <T extends TileEntity> Optional<T> findTileEntityClient(DimensionType dimension, BlockPos pos, Class<T> c){
        return Minecraft.getInstance().world.getDimension().getType().equals(dimension)
            ? findTileEntity(Minecraft.getInstance().world, pos, c)
            : Optional.empty();
    }

}
