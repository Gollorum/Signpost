package gollorum.signpost.utils.serialization;

import gollorum.signpost.utils.Either;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class WorldSerializer implements CompoundSerializable<Either<Level, ResourceLocation>> {

    public static final WorldSerializer INSTANCE = new WorldSerializer();

    private WorldSerializer(){}

    @Override
    public void encode(CompoundTag compound, Either<Level, ResourceLocation> world, HolderLookup.Provider provider) {
        compound.putString("DimensionId", world.rightOr(w -> w.dimension().location()).toString());
        return compound;
    }

    @Override
    public Either<Level, ResourceLocation> decode(CompoundTag compound, HolderLookup.Provider provider) {
        return Either.right(ResourceLocation.parse(compound.getString("DimensionId")));
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
    public void encode(FriendlyByteBuf buffer, Either<Level, ResourceLocation> world) {
        buffer.writeResourceLocation(world.rightOr(w -> w.dimension().location()));
    }

    @Override
    public Either<Level, ResourceLocation> decode(FriendlyByteBuf buffer) {
        return Either.right(buffer.readResourceLocation());
    }

}
