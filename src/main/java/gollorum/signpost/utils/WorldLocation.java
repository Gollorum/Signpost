package gollorum.signpost.utils;

import gollorum.signpost.utils.serialization.BlockPosSerializer;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.WorldSerializer;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class WorldLocation {

    public static Optional<WorldLocation> from(@Nullable TileEntity tile) {
        return tile != null && tile.hasLevel()
            ? Optional.of(new WorldLocation(tile.getBlockPos(), tile.getLevel()))
            : Optional.empty();
    }

    public final BlockPos blockPos;
    public final Either<World, ResourceLocation> world;

    public WorldLocation(BlockPos blockPos, Either<World, ResourceLocation> world) {
        this.blockPos = blockPos;
        this.world = world;
    }

    public WorldLocation(BlockPos blockPos, World world) {
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
            world.match(World::gatherChunkSourceStats, ResourceLocation::toString)
        );
    }

    public static final Serializer SERIALIZER = new Serializer();

    public static final class Serializer implements CompoundSerializable<WorldLocation>{

        @Override
        public CompoundNBT write(WorldLocation worldLocation, CompoundNBT compound) {
            compound.put("Pos", BlockPosSerializer.INSTANCE.write(worldLocation.blockPos));
            compound.put("World", WorldSerializer.INSTANCE.write(worldLocation.world));
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundNBT compound) {
            return compound.contains("Pos")
                && BlockPosSerializer.INSTANCE.isContainedIn(compound.getCompound("Pos"))
                && compound.contains("World")
                && WorldSerializer.INSTANCE.isContainedIn(compound.getCompound("World"));
        }

        @Override
        public WorldLocation read(CompoundNBT compound) {
            return new WorldLocation(
                BlockPosSerializer.INSTANCE.read(compound.getCompound("Pos")),
                WorldSerializer.INSTANCE.read(compound.getCompound("World"))
            );
        }

        @Override
        public Class<WorldLocation> getTargetClass() {
            return WorldLocation.class;
        }

        @Override
        public void write(WorldLocation worldLocation, PacketBuffer buffer) {
            BlockPosSerializer.INSTANCE.write(worldLocation.blockPos, buffer);
            WorldSerializer.INSTANCE.write(worldLocation.world, buffer);
        }

        @Override
        public WorldLocation read(PacketBuffer buffer) {
            return new WorldLocation(
                BlockPosSerializer.INSTANCE.read(buffer),
                WorldSerializer.INSTANCE.read(buffer)
            );
        }
    }
}
