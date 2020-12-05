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
        return tile != null && tile.hasWorld()
            ? Optional.of(new WorldLocation(tile.getPos(), tile.getWorld()))
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
            world.rightOr(w -> w.getDimensionKey().getLocation())
                .equals(that.world.rightOr(w -> w.getDimensionKey().getLocation()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockPos, world.rightOr(w -> w.getDimensionKey().getLocation()));
    }

    public static final Serializer SERIALIZER = new Serializer();

    public static final class Serializer implements CompoundSerializable<WorldLocation>{

        @Override
        public void writeTo(WorldLocation worldLocation, CompoundNBT compound, String keyPrefix) {
            BlockPosSerializer.INSTANCE.writeTo(worldLocation.blockPos, compound, keyPrefix + "Pos");
            WorldSerializer.INSTANCE.writeTo(worldLocation.world, compound, keyPrefix + "World");
        }

        @Override
        public boolean isContainedIn(CompoundNBT compound, String keyPrefix) {
            return BlockPosSerializer.INSTANCE.isContainedIn(compound, keyPrefix + "Pos") &&
                WorldSerializer.INSTANCE.isContainedIn(compound, keyPrefix + "World");
        }

        @Override
        public WorldLocation read(CompoundNBT compound, String keyPrefix) {
            return new WorldLocation(
                BlockPosSerializer.INSTANCE.read(compound, keyPrefix + "Pos"),
                WorldSerializer.INSTANCE.read(compound, keyPrefix + "World")
            );
        }

        @Override
        public void writeTo(WorldLocation worldLocation, PacketBuffer buffer) {
            BlockPosSerializer.INSTANCE.writeTo(worldLocation.blockPos, buffer);
            WorldSerializer.INSTANCE.writeTo(worldLocation.world, buffer);
        }

        @Override
        public WorldLocation readFrom(PacketBuffer buffer) {
            return new WorldLocation(
                BlockPosSerializer.INSTANCE.readFrom(buffer),
                WorldSerializer.INSTANCE.readFrom(buffer)
            );
        }
    }
}
