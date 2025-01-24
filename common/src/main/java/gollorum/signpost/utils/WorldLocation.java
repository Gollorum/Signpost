package gollorum.signpost.utils;

import gollorum.signpost.utils.serialization.BlockPosSerializer;
import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.WorldSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
        this.world = world.mapRight(loc -> loc.getPath().equals("") ? Level.OVERWORLD.location() : loc);
    }

    public WorldLocation(BlockPos blockPos, Level world) {
        this(blockPos, Either.left(world));
    }

    public WorldLocation(BlockPos blockPos, ResourceLocation dimensionKeyLocation) {
        this(blockPos, Either.right(dimensionKeyLocation));
    }

    public WorldLocation withoutExplicitLevel() {
        if(world.isLeft()) {
            return new WorldLocation(blockPos, Either.right(world.leftOrThrow().dimension().location()));
        } else {
            return this;
        }
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

    public static final CompoundSerializable<WorldLocation> COMPOUND_SERIALIZER = new CompoundSerializable<WorldLocation>() {

        @Override
        public void encode(CompoundTag compound, WorldLocation worldLocation, HolderLookup.Provider registryAccess) {
            compound.put("Pos", BlockPosSerializer.COMPOUND.encode(worldLocation.blockPos, registryAccess));
            compound.put("Level", WorldSerializer.COMPOUND.encode(worldLocation.world, registryAccess));
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return compound.contains("Pos")
                && BlockPosSerializer.COMPOUND.isContainedIn(compound.getCompound("Pos"))
                && compound.contains("Level")
                && WorldSerializer.COMPOUND.isContainedIn(compound.getCompound("Level"));
        }

        @Override
        public WorldLocation decode(CompoundTag compound, HolderLookup.Provider registryAccess) {
            return new WorldLocation(
                BlockPosSerializer.COMPOUND.decode(compound.getCompound("Pos"), registryAccess),
                WorldSerializer.COMPOUND.decode(compound.getCompound("Level"), registryAccess)
            );
        }
    };

    public static final BufferSerializable<WorldLocation> BUFFER_SERIALIZER = new BufferSerializable<WorldLocation>() {


        @Override
        public Class<WorldLocation> getTargetClass() {
            return WorldLocation.class;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, WorldLocation worldLocation) {
            BlockPosSerializer.BUFFER.encode(buffer, worldLocation.blockPos);
            WorldSerializer.BUFFER.encode(buffer, worldLocation.world);
        }

        @Override
        public WorldLocation decode(RegistryFriendlyByteBuf buffer) {
            return new WorldLocation(
                BlockPosSerializer.BUFFER.decode(buffer),
                WorldSerializer.BUFFER.decode(buffer)
            );
        }
    };
}
