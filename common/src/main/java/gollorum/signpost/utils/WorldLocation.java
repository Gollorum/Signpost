package gollorum.signpost.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.utils.serialization.BlockPosSerializer;
import gollorum.signpost.utils.serialization.WorldSerializer;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;
import java.util.Optional;

public record WorldLocation(BlockPos blockPos, Either<Level, ResourceLocation> world) {

    public static Optional<WorldLocation> from(BlockEntity tile) {
        return tile != null && tile.hasLevel()
            ? Optional.of(new WorldLocation(tile.getBlockPos(), tile.getLevel()))
            : Optional.empty();
    }


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

    public static final Codec<WorldLocation> CODEC = RecordCodecBuilder.create(i -> i.group(
        BlockPosSerializer.CODEC.fieldOf("Pos").forGetter(WorldLocation::blockPos),
        WorldSerializer.MAP_CODEC.fieldOf("Level").forGetter(WorldLocation::world)
    ).apply(i, WorldLocation::new));

    public static final StreamCodec<ByteBuf, WorldLocation> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, WorldLocation::blockPos,
        WorldSerializer.STREAM_CODEC, WorldLocation::world,
        WorldLocation::new
    );

}
