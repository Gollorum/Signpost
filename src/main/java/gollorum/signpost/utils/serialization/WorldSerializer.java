package gollorum.signpost.utils.serialization;

import gollorum.signpost.utils.Either;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class WorldSerializer implements CompoundSerializable<Either<Level, ResourceLocation>> {

    public static final WorldSerializer INSTANCE = new WorldSerializer();

    private WorldSerializer(){}

    @Override
    public CompoundTag write(Either<Level, ResourceLocation> world, CompoundTag compound) {
        compound.putString("DimensionId", world.rightOr(w -> w.dimension().location()).toString());
        return compound;
    }

    @Override
    public Either<Level, ResourceLocation> read(CompoundTag compound) {
        return Either.right(new ResourceLocation(compound.getString("DimensionId")));
    }

    @Override
    public boolean isContainedIn(CompoundTag compound) {
        return compound.contains("DimensionId");
    }

    @Override
    public Class<Either<Level, ResourceLocation>> getTargetClass() {
        return (Class<Either<Level, ResourceLocation>>) Either.<Level, ResourceLocation>right(null).getClass();
    }

    @Override
    public void write(Either<Level, ResourceLocation> world, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(world.rightOr(w -> w.dimension().location()));
    }

    @Override
    public Either<Level, ResourceLocation> read(FriendlyByteBuf buffer) {
        return Either.right(buffer.readResourceLocation());
    }

}
