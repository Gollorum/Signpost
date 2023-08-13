package gollorum.signpost.minecraft.utils.tints;

import gollorum.signpost.utils.Tint;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
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
        public CompoundTag write(GrassTint grassTint, CompoundTag compound) {
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return true;
        }

        @Override
        public GrassTint read(CompoundTag compound) {
            return new GrassTint();
        }

        @Override
        public void write(GrassTint grassTint, FriendlyByteBuf buffer) {
        }

        @Override
        public GrassTint read(FriendlyByteBuf buffer) {
            return new GrassTint();
        }

        @Override
        public Class<GrassTint> getTargetClass() {
            return GrassTint.class;
        }
    };

}
