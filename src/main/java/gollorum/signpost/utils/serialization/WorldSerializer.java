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
    public CompoundNBT write(Either<World, ResourceLocation> world, CompoundNBT compound) {
        compound.putString("DimensionId", world.rightOr(w -> w.getDimension().getType().getRegistryName()).toString());
        return compound;
    }

    @Override
    public Either<World, ResourceLocation> read(CompoundNBT compound) {
        return Either.right(new ResourceLocation(compound.getString("DimensionId")));
    }

    @Override
    public boolean isContainedIn(CompoundNBT compound) {
        return compound.contains("DimensionId");
    }

    @Override
    public Class<Either<World, ResourceLocation>> getTargetClass() {
        return (Class<Either<World, ResourceLocation>>) Either.<World, ResourceLocation>right(null).getClass();
    }

    @Override
    public void write(Either<World, ResourceLocation> world, PacketBuffer buffer) {
        buffer.writeResourceLocation(world.rightOr(w -> w.getDimension().getType().getRegistryName()));
    }

    @Override
    public Either<World, ResourceLocation> read(PacketBuffer buffer) {
        return Either.right(buffer.readResourceLocation());
    }

}
