package gollorum.signpost.minecraft.utils;

import gollorum.signpost.Signpost;
import gollorum.signpost.utils.Delay;
import gollorum.signpost.utils.Either;
import gollorum.signpost.utils.WorldLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.function.Consumer;

public class TileEntityUtils {

    public static <T> Optional<T> findTileEntity(IBlockReader world, BlockPos pos, Class<T> c){
        TileEntity tileEntity = world.getBlockEntity(pos);
        if(tileEntity != null && c.isAssignableFrom(tileEntity.getClass())){
            return Optional.of((T) tileEntity);
        } else return Optional.empty();
    }

    public static <T> void delayUntilTileEntityExists(IWorld world, BlockPos pos, Class<T> c, Consumer<T> action, int timeout, Optional<Runnable> onTimeOut) {
        Delay.untilIsPresent(() -> findTileEntity(world, pos, c), action, timeout, world.isClientSide(), onTimeOut);
    }

    public static <T> Optional<T> findTileEntity(ResourceLocation dimensionKeyLocation, boolean isRemote, BlockPos blockPos, Class<T> c){
        return findWorld(dimensionKeyLocation, isRemote).flatMap(world -> findTileEntity(world, blockPos, c));
    }

    public static Optional<World> findWorld(ResourceLocation dimensionKeyLocation, boolean isClient) {
        return isClient
            ? (Minecraft.getInstance().level.dimension().location().equals(dimensionKeyLocation)
                ? Optional.of(Minecraft.getInstance().level)
                : Optional.empty())
            : (Signpost.getServerType().isServer
                ? Optional.ofNullable(Signpost.getServerInstance()
                    .getLevel(RegistryKey.create(Registry.DIMENSION_REGISTRY, dimensionKeyLocation)))
                : Optional.empty());
    }

    public static Optional<World> toWorld(Either<World, ResourceLocation> either, boolean onClient) {
        return either.match(
            Optional::of,
            right -> findWorld(right, onClient)
        );
    }

    public static <T> Optional<T> findTileEntityAt(WorldLocation location, Class<T> c, boolean onClient) {
        return toWorld(location.world, onClient)
            .map(w -> w.getBlockEntity(location.blockPos))
            .flatMap(tile -> c.isAssignableFrom(tile.getClass()) ? Optional.of((T)tile) : Optional.empty());
    }

    public static <T> void delayUntilTileEntityExistsAt(WorldLocation location, Class<T> c, Consumer<T> action, int timeout, boolean onClient, Optional<Runnable> onTimeOut) {
        Delay.untilIsPresent(() -> findTileEntityAt(location, c, onClient), action, timeout, onClient, onTimeOut);
    }

    public static <T> Optional<T> findTileEntityClient(ResourceLocation dimensionKeyLocation, BlockPos pos, Class<T> c){
        return Minecraft.getInstance().level.dimension().location().equals(dimensionKeyLocation)
            ? findTileEntity(Minecraft.getInstance().level, pos, c)
            : Optional.empty();
    }

}
