package gollorum.signpost.utils.serialization;

import gollorum.signpost.utils.Either;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

public class WorldSerializer implements CompoundSerializable<Either<World, Integer>> {

    public static final WorldSerializer INSTANCE = new WorldSerializer();

    private WorldSerializer(){}

    @Override
    public void writeTo(Either<World, Integer> world, CompoundNBT compound, String keyPrefix) {
        compound.putInt(keyPrefix + "DimensionId", world.rightOr(w -> w.dimension.getType().getId()));
    }

    @Override
    public Either<World, Integer> read(CompoundNBT compound, String keyPrefix) {
        return Either.right(compound.getInt(keyPrefix + "DimensionId"));
    }

    @Override
    public void writeTo(Either<World, Integer> world, PacketBuffer buffer) {
        buffer.writeInt(world.rightOr(w -> w.dimension.getType().getId()));
    }

    @Override
    public Either<World, Integer> readFrom(PacketBuffer buffer) {
        return Either.right(buffer.readInt());
    }

}
