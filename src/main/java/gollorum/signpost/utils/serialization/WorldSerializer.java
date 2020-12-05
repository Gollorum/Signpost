package gollorum.signpost.utils.serialization;

import gollorum.signpost.utils.Either;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class WorldSerializer implements CompoundSerializable<Either<World, ResourceLocation>> {

    public static final WorldSerializer INSTANCE = new WorldSerializer();

    private WorldSerializer(){}

    @Override
    public void writeTo(Either<World, ResourceLocation> world, CompoundNBT compound, String keyPrefix) {
        compound.putString(keyPrefix + "DimensionId", world.rightOr(w -> w.getDimensionKey().getLocation()).toString());
    }

    @Override
    public Either<World, ResourceLocation> read(CompoundNBT compound, String keyPrefix) {
        return Either.right(new ResourceLocation(compound.getString(keyPrefix + "DimensionId")));
    }

    @Override
    public boolean isContainedIn(CompoundNBT compound, String keyPrefix) {
        return compound.contains(keyPrefix + "DimensionId");
    }

    @Override
    public void writeTo(Either<World, ResourceLocation> world, PacketBuffer buffer) {
        buffer.writeResourceLocation(world.rightOr(w -> w.getDimensionKey().getLocation()));
    }

    @Override
    public Either<World, ResourceLocation> readFrom(PacketBuffer buffer) {
        return Either.right(buffer.readResourceLocation());
    }

}
