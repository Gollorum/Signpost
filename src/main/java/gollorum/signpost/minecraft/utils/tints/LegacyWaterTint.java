package gollorum.signpost.minecraft.utils.tints;

import gollorum.signpost.utils.Tint;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.BlockAndTintGetter;

public class LegacyWaterTint implements Tint {

    @Override
    public int getColorAt(BlockAndTintGetter level, BlockPos pos) {
        return BiomeColors.getAverageWaterColor(level, pos);
    }

    public static void register() {
        Tint.Serialization.register("water", serializer);
    }

    public static final CompoundSerializable<LegacyWaterTint> serializer = new CompoundSerializable<>() {
        @Override
        public CompoundTag write(LegacyWaterTint waterTint, CompoundTag compound) {
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return true;
        }

        @Override
        public LegacyWaterTint read(CompoundTag compound) {
            return new LegacyWaterTint();
        }

        @Override
        public void write(LegacyWaterTint waterTint, FriendlyByteBuf buffer) {
        }

        @Override
        public LegacyWaterTint read(FriendlyByteBuf buffer) {
            return new LegacyWaterTint();
        }

        @Override
        public Class<LegacyWaterTint> getTargetClass() {
            return LegacyWaterTint.class;
        }
    };

}
