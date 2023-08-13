package gollorum.signpost.minecraft.utils.tints;

import gollorum.signpost.utils.Tint;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.BlockAndTintGetter;

public class WaterTint implements Tint {

    @Override
    public int getColorAt(BlockAndTintGetter level, BlockPos pos) {
        return BiomeColors.getAverageWaterColor(level, pos);
    }

    public static void register() {
        Tint.Serialization.register("water", serializer);
    }

    public static final CompoundSerializable<WaterTint> serializer = new CompoundSerializable<>() {
        @Override
        public CompoundTag write(WaterTint waterTint, CompoundTag compound) {
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return true;
        }

        @Override
        public WaterTint read(CompoundTag compound) {
            return new WaterTint();
        }

        @Override
        public void write(WaterTint waterTint, FriendlyByteBuf buffer) {
        }

        @Override
        public WaterTint read(FriendlyByteBuf buffer) {
            return new WaterTint();
        }

        @Override
        public Class<WaterTint> getTargetClass() {
            return WaterTint.class;
        }
    };

}
