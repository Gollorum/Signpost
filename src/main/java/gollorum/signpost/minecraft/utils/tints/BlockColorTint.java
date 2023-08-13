package gollorum.signpost.minecraft.utils.tints;

import gollorum.signpost.utils.Tint;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.ResourceLocationSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

public record BlockColorTint(Block block, int tintIndex) implements Tint {

    @Override
    public int getColorAt(BlockAndTintGetter level, BlockPos pos) {
        return net.minecraft.client.Minecraft.getInstance().getBlockColors().getColor(block.defaultBlockState(), level, pos, tintIndex);
    }

    public static void register() {
        Tint.Serialization.register("blockColor", serializer);
    }

    public static final CompoundSerializable<BlockColorTint> serializer = new CompoundSerializable<>() {
        @Override
        public CompoundTag write(BlockColorTint tint, CompoundTag compound) {
            ResourceLocationSerializer.Instance.write(Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(tint.block)), compound);
            compound.putInt("TintIndex", tint.tintIndex);
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return ResourceLocationSerializer.Instance.isContainedIn(compound);
        }

        @Override
        public BlockColorTint read(CompoundTag compound) {
            return new BlockColorTint(
                ForgeRegistries.BLOCKS.getValue(ResourceLocationSerializer.Instance.read(compound)),
                compound.getInt("TintIndex")
            );
        }

        @Override
        public void write(BlockColorTint tint, FriendlyByteBuf buffer) {
            ResourceLocationSerializer.Instance.write(Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(tint.block)), buffer);
            buffer.writeInt(tint.tintIndex);
        }

        @Override
        public BlockColorTint read(FriendlyByteBuf buffer) {
            return new BlockColorTint(
                ForgeRegistries.BLOCKS.getValue(ResourceLocationSerializer.Instance.read(buffer)),
                buffer.readInt()
            );
        }

        @Override
        public Class<BlockColorTint> getTargetClass() {
            return BlockColorTint.class;
        }
    };

}
