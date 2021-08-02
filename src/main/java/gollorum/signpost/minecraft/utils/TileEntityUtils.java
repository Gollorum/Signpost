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
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.function.Consumer;

public class TileEntityUtils {

    public static <T> Optional<T> findTileEntity(IWorld world, BlockPos pos, Class<T> c){
        TileEntity tileEntity = world.getTileEntity(pos);
        if(tileEntity != null && c.isAssignableFrom(tileEntity.getClass())){
            return Optional.of((T) tileEntity);
        } else return Optional.empty();
    }

    public static <T> void delayUntilTileEntityExists(IWorld world, BlockPos pos, Class<T> c, Consumer<T> action, int timeout, Optional<Runnable> onTimeOut) {
        Delay.untilIsPresent(() -> findTileEntity(world, pos, c), action, timeout, world.isRemote(), onTimeOut);
    }

    public static <T> Optional<T> findTileEntity(ResourceLocation dimensionKeyLocation, boolean isRemote, BlockPos blockPos, Class<T> c){
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

    public static Optional<World> toWorld(Either<World, ResourceLocation> either) {
        return either.match(
            Optional::of,
            right -> findWorld(right, !Signpost.getServerType().isServer)
        );
    }

    public static <T> Optional<T> findTileEntityAt(WorldLocation location, Class<T> c) {
        return toWorld(location.world)
            .map(w -> w.getTileEntity(location.blockPos))
            .flatMap(tile -> c.isAssignableFrom(tile.getClass()) ? Optional.of((T)tile) : Optional.empty());
    }

    public static <T> void delayUntilTileEntityExistsAt(WorldLocation location, Class<T> c, Consumer<T> action, int timeout, Optional<Runnable> onTimeOut) {
        Delay.untilIsPresent(() -> findTileEntityAt(location, c), action, timeout, onTimeOut);
    }

    public static <T> Optional<T> findTileEntityClient(ResourceLocation dimensionKeyLocation, BlockPos pos, Class<T> c){
        return Minecraft.getInstance().world.getDimensionKey().getLocation().equals(dimensionKeyLocation)
            ? findTileEntity(Minecraft.getInstance().world, pos, c)
            : Optional.empty();
    }

}
