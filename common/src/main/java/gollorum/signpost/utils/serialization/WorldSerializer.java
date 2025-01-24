package gollorum.signpost.utils.serialization;

import gollorum.signpost.utils.Either;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class WorldSerializer {

    public static final CompoundSerializable<Either<Level, ResourceLocation>> COMPOUND = new CompoundSerializable<>() {
    
        @Override
        public void encode(CompoundTag compound, Either<Level, ResourceLocation> world, HolderLookup.Provider provider) {
            compound.putString("DimensionId", world.rightOr(w -> w.dimension().location()).toString());
        }
    
        @Override
        public Either<Level, ResourceLocation> decode(CompoundTag compound, HolderLookup.Provider provider) {
            return Either.right(ResourceLocation.parse(compound.getString("DimensionId")));
        }
    
        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return compound.contains("DimensionId");
        }
    };

    public static final BufferSerializable<Either<Level, ResourceLocation>> BUFFER = new BufferSerializable<>() {

        @Override
        public Class<Either<Level, ResourceLocation>> getTargetClass() {
            return (Class<Either<Level, ResourceLocation>>) Either.<Level, ResourceLocation>right(null).getClass();
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, Either<Level, ResourceLocation> world) {
            buffer.writeResourceLocation(world.rightOr(w -> w.dimension().location()));
        }

        @Override
        public Either<Level, ResourceLocation> decode(RegistryFriendlyByteBuf buffer) {
            return Either.right(buffer.readResourceLocation());
        }
    };

}
