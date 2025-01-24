package gollorum.signpost.minecraft.utils.tints;

import gollorum.signpost.utils.Tint;
import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.BlockAndTintGetter;

public class FoliageTint implements Tint {

    @Override
    public int getColorAt(BlockAndTintGetter level, BlockPos pos) {
        return BiomeColors.getAverageFoliageColor(level, pos);
    }

    public static void register() {
        Tint.Serialization.register("foliage", new Serializer(compoundSerializer, bufferSerializer));
    }

    public static final CompoundSerializable<FoliageTint> compoundSerializer = new CompoundSerializable<>() {
        @Override
        public void encode(CompoundTag compound, FoliageTint foliageTint, HolderLookup.Provider provider) { }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return true;
        }

        @Override
        public FoliageTint decode(CompoundTag compound, HolderLookup.Provider provider) {
            return new FoliageTint();
        }
    };

    public static final BufferSerializable<FoliageTint> bufferSerializer = new BufferSerializable<>() {

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, FoliageTint foliageTint) {
        }

        @Override
        public FoliageTint decode(RegistryFriendlyByteBuf buffer) {
            return new FoliageTint();
        }

        @Override
        public Class<FoliageTint> getTargetClass() {
            return FoliageTint.class;
        }
    };

}
