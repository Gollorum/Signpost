package gollorum.signpost.minecraft.utils.tints;

import gollorum.signpost.utils.Tint;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.BlockAndTintGetter;

public class FoliageTint implements Tint {

    @Override
    public int getColorAt(BlockAndTintGetter level, BlockPos pos) {
        return BiomeColors.getAverageFoliageColor(level, pos);
    }

    public static void register() {
        Tint.Serialization.register("foliage", serializer);
    }

    public static final CompoundSerializable<FoliageTint> serializer = new CompoundSerializable<>() {
        @Override
        public void encode(CompoundTag compound, FoliageTint foliageTint, HolderLookup.Provider provider) {
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return true;
        }

        @Override
        public FoliageTint decode(CompoundTag compound, HolderLookup.Provider provider) {
            return new FoliageTint();
        }

        @Override
        public void encode(FriendlyByteBuf buffer, FoliageTint foliageTint) {
        }

        @Override
        public FoliageTint decode(FriendlyByteBuf buffer) {
            return new FoliageTint();
        }

        @Override
        public Class<FoliageTint> getTargetClass() {
            return FoliageTint.class;
        }
    };

}
