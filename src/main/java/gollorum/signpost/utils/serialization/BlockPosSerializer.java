package gollorum.signpost.utils.serialization;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class BlockPosSerializer implements CompoundSerializable<BlockPos> {

    public static final BlockPosSerializer INSTANCE = new BlockPosSerializer();

    private BlockPosSerializer(){}

    @Override
    public CompoundTag write(BlockPos blockPos, CompoundTag compound) {
        compound.putInt("X", blockPos.getX());
        compound.putInt("Y", blockPos.getY());
        compound.putInt("Z", blockPos.getZ());
        return compound;
    }

    @Override
    public boolean isContainedIn(CompoundTag compound) {
        return compound.contains("X") &&
            compound.contains("Y") &&
            compound.contains("Z");
    }

    @Override
    public BlockPos read(CompoundTag compound) {
        return new BlockPos(
            compound.getInt("X"),
            compound.getInt("Y"),
            compound.getInt("Z")
        );
    }

    @Override
    public Class<BlockPos> getTargetClass() {
        return BlockPos.class;
    }

    @Override
    public void write(BlockPos blockPos, FriendlyByteBuf buffer) {
        buffer.writeInt(blockPos.getX());
        buffer.writeInt(blockPos.getY());
        buffer.writeInt(blockPos.getZ());
    }

    @Override
    public BlockPos read(FriendlyByteBuf buffer) {
        return new BlockPos(
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt()
        );
    }
}
