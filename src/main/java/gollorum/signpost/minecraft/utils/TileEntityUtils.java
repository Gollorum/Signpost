package gollorum.signpost.minecraft.utils;

import gollorum.signpost.Signpost;
import gollorum.signpost.utils.Delay;
import gollorum.signpost.utils.Either;
import gollorum.signpost.utils.WorldLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CommonLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Optional;
import java.util.function.Consumer;

public class TileEntityUtils {

    public static <T extends BlockEntity> Optional<T> findTileEntity(BlockGetter level, BlockPos pos, BlockEntityType<T> c) {
        if(level instanceof Level l && l.thread != Thread.currentThread())
            throw new RuntimeException("Tried to access block entity from wrong thread");
        return level.getBlockEntity(pos, c);
    }

    public static BlockEntity findTileEntity(BlockGetter level, BlockPos pos) {
        if(level instanceof Level l && l.thread != Thread.currentThread())
            throw new RuntimeException("Tried to access block entity from wrong thread");
        return level.getBlockEntity(pos);
    }

    public static <T extends BlockEntity> void delayUntilTileEntityExists(LevelAccessor world, BlockPos pos, BlockEntityType<T> c, Consumer<T> action, int timeout, Optional<Runnable> onTimeOut) {
        Delay.untilIsPresent(
            () -> world instanceof Level l && l.thread != Thread.currentThread() ? Optional.empty() : findTileEntity(world, pos, c),
            action,
            timeout,
            world.isClientSide(),
            onTimeOut
        );
    }

    public static <T extends BlockEntity> Optional<T> findTileEntity(ResourceLocation dimensionKeyLocation, boolean isRemote, BlockPos blockPos, BlockEntityType<T> c){
        return findWorld(dimensionKeyLocation, isRemote).flatMap(world -> findTileEntity(world, blockPos, c));
    }

    public static Optional<Level> findWorld(ResourceLocation dimensionKeyLocation, boolean isClient) {
        return isClient
            ? (Minecraft.getInstance().level.dimension().location().equals(dimensionKeyLocation)
                ? Optional.of(Minecraft.getInstance().level)
                : Optional.empty())
            : (Signpost.getServerType().isServer
                ? Optional.ofNullable(Signpost.getServerInstance()
                    .getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY, dimensionKeyLocation)))
                : Optional.empty());
    }

    public static Optional<Level> toWorld(Either<Level, ResourceLocation> either, boolean onClient) {
        return either.match(
            Optional::of,
            right -> findWorld(right, onClient)
        );
    }

    public static <T> Optional<T> findTileEntityAt(WorldLocation location, Class<T> c, boolean onClient) {
        return toWorld(location.world, onClient)
            .map(w -> TileEntityUtils.findTileEntity(w, location.blockPos))
            .flatMap(tile -> c.isAssignableFrom(tile.getClass()) ? Optional.of((T)tile) : Optional.empty());
    }

    public static <T extends BlockEntity> Optional<T> findTileEntityAt(WorldLocation location, BlockEntityType<T> c, boolean onClient) {
        return toWorld(location.world, onClient)
            .flatMap(w -> findTileEntity(w, location.blockPos, c));
    }

    public static <T> void delayUntilTileEntityExistsAt(WorldLocation location, Class<T> c, Consumer<T> action, int timeout, boolean onClient, Optional<Runnable> onTimeOut) {
        Delay.untilIsPresent(() -> findTileEntityAt(location, c, onClient), action, timeout, onClient, onTimeOut);
    }

    public static <T extends BlockEntity> void delayUntilTileEntityExistsAt(WorldLocation location, BlockEntityType<T> c, Consumer<T> action, int timeout, boolean onClient, Optional<Runnable> onTimeOut) {
        Delay.untilIsPresent(() -> findTileEntityAt(location, c, onClient), action, timeout, onClient, onTimeOut);
    }

    public static <T extends BlockEntity> Optional<T> findTileEntityClient(ResourceLocation dimensionKeyLocation, BlockPos pos, BlockEntityType<T> c){
        return Minecraft.getInstance().level.dimension().location().equals(dimensionKeyLocation)
            ? findTileEntity(Minecraft.getInstance().level, pos, c)
            : Optional.empty();
    }

}
