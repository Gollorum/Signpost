package gollorum.signpost.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.utils.serialization.BlockPosSerializer;
import gollorum.signpost.utils.serialization.WorldSerializer;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;
import java.util.Optional;

public record WorldLocation(BlockPos blockPos, Either<Level, Identifier> world) {

    public static Optional<WorldLocation> from(BlockEntity tile) {
        return tile != null && tile.hasLevel()
            ? Optional.of(WorldLocation.from(tile.getBlockPos(), tile.getLevel()))
            : Optional.empty();
    }

    public static WorldLocation from(BlockPos blockPos, Either<Level, Identifier> world) {
        return new WorldLocation(
            blockPos,
            world.mapRight(loc -> loc.getPath().equals("") ? Level.OVERWORLD.identifier() : loc)
        );
    }

    public static WorldLocation from(BlockPos blockPos, Level world) {
        return from(blockPos, Either.left(world));
    }

    public static WorldLocation from(BlockPos blockPos, Identifier dimensionKeyLocation) {
        return from(blockPos, Either.right(dimensionKeyLocation));
    }

    public WorldLocation withoutExplicitLevel() {
        if(world.isLeft()) {
            return WorldLocation.from(blockPos, Either.right(world.leftOrThrow().dimension().identifier()));
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
            world.rightOr(w -> w.dimension().identifier())
                .equals(that.world.rightOr(w -> w.dimension().identifier()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockPos, world.rightOr(w -> w.dimension().identifier()));
    }

    @Override
    public String toString() {
        return String.format("(%d %d %d) in %s",
            blockPos.getX(), blockPos.getY(), blockPos.getZ(),
            world.match(Level::gatherChunkSourceStats, Identifier::toString)
        );
    }

    public static final Codec<WorldLocation> CODEC = RecordCodecBuilder.create(i -> i.group(
        BlockPosSerializer.CODEC.fieldOf("Pos").forGetter(WorldLocation::blockPos),
        WorldSerializer.MAP_CODEC.fieldOf("Level").forGetter(WorldLocation::world)
    ).apply(i, WorldLocation::from));

    public static final StreamCodec<ByteBuf, WorldLocation> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, WorldLocation::blockPos,
        WorldSerializer.STREAM_CODEC, WorldLocation::world,
        WorldLocation::from
    );

}
