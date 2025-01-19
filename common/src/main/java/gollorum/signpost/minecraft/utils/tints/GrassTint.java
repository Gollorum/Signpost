package gollorum.signpost.minecraft.utils.tints;

import gollorum.signpost.utils.Tint;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.BlockAndTintGetter;

public class GrassTint implements Tint {

    @Override
    public int getColorAt(BlockAndTintGetter level, BlockPos pos) {
        return BiomeColors.getAverageGrassColor(level, pos);
    }

    public static void register() {
        Tint.Serialization.register("grass", serializer);
    }

    public static final CompoundSerializable<GrassTint> serializer = new CompoundSerializable<>() {
        @Override
        public void encode(CompoundTag compound, GrassTint grassTint, HolderLookup.Provider provider) {
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return true;
        }

        @Override
        public GrassTint decode(CompoundTag compound, HolderLookup.Provider provider) {
            return new GrassTint();
        }

        @Override
        public void encode(FriendlyByteBuf buffer, GrassTint grassTint) {
        }

        @Override
        public GrassTint decode(FriendlyByteBuf buffer) {
            return new GrassTint();
        }

        @Override
        public Class<GrassTint> getTargetClass() {
            return GrassTint.class;
        }
    };

}
