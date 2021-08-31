package gollorum.signpost.utils;

import gollorum.signpost.utils.serialization.BlockPosSerializer;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.WorldSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class WorldLocation {

    public static Optional<WorldLocation> from(@Nullable BlockEntity tile) {
        return tile != null && tile.hasLevel()
            ? Optional.of(new WorldLocation(tile.getBlockPos(), tile.getLevel()))
            : Optional.empty();
    }

    public final BlockPos blockPos;
    public final Either<Level, ResourceLocation> world;

    public WorldLocation(BlockPos blockPos, Either<Level, ResourceLocation> world) {
        this.blockPos = blockPos;
        this.world = world;
    }

    public WorldLocation(BlockPos blockPos, Level world) {
        this.blockPos = blockPos;
        this.world = Either.left(world);
    }

    public WorldLocation(BlockPos blockPos, ResourceLocation dimensionKeyLocation) {
        this.blockPos = blockPos;
        this.world = Either.right(dimensionKeyLocation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldLocation that = (WorldLocation) o;
        return blockPos.equals(that.blockPos) &&
            world.rightOr(w -> w.dimension().location())
                .equals(that.world.rightOr(w -> w.dimension().location()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockPos, world.rightOr(w -> w.dimension().location()));
    }

    @Override
    public String toString() {
        return String.format("(%d %d %d) in %s",
            blockPos.getX(), blockPos.getY(), blockPos.getZ(),
            world.match(Level::gatherChunkSourceStats, ResourceLocation::toString)
        );
    }

    public static final Serializer SERIALIZER = new Serializer();

    public static final class Serializer implements CompoundSerializable<WorldLocation>{

        @Override
        public CompoundTag write(WorldLocation worldLocation, CompoundTag compound) {
            compound.put("Pos", BlockPosSerializer.INSTANCE.write(worldLocation.blockPos));
            compound.put("Level", WorldSerializer.INSTANCE.write(worldLocation.world));
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return compound.contains("Pos")
                && BlockPosSerializer.INSTANCE.isContainedIn(compound.getCompound("Pos"))
                && compound.contains("Level")
                && WorldSerializer.INSTANCE.isContainedIn(compound.getCompound("Level"));
        }

        @Override
        public WorldLocation read(CompoundTag compound) {
            return new WorldLocation(
                BlockPosSerializer.INSTANCE.read(compound.getCompound("Pos")),
                WorldSerializer.INSTANCE.read(compound.getCompound("Level"))
            );
        }

        @Override
        public Class<WorldLocation> getTargetClass() {
            return WorldLocation.class;
        }

        @Override
        public void write(WorldLocation worldLocation, FriendlyByteBuf buffer) {
            BlockPosSerializer.INSTANCE.write(worldLocation.blockPos, buffer);
            WorldSerializer.INSTANCE.write(worldLocation.world, buffer);
        }

        @Override
        public WorldLocation read(FriendlyByteBuf buffer) {
            return new WorldLocation(
                BlockPosSerializer.INSTANCE.read(buffer),
                WorldSerializer.INSTANCE.read(buffer)
            );
        }
    }
}
