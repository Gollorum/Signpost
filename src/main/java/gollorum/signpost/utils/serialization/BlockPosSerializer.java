package gollorum.signpost.utils.serialization;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

public class BlockPosSerializer implements CompoundSerializable<BlockPos> {

    public static final BlockPosSerializer INSTANCE = new BlockPosSerializer();

    private BlockPosSerializer(){}

    @Override
    public CompoundNBT write(BlockPos blockPos, CompoundNBT compound) {
        compound.putInt("X", blockPos.getX());
        compound.putInt("Y", blockPos.getY());
        compound.putInt("Z", blockPos.getZ());
        return compound;
    }

    @Override
    public boolean isContainedIn(CompoundNBT compound) {
        return compound.contains("X") &&
            compound.contains("Y") &&
            compound.contains("Z");
    }

    @Override
    public BlockPos read(CompoundNBT compound) {
        return new BlockPos(
            compound.getInt("X"),
            compound.getInt("Y"),
            compound.getInt("Z")
        );
    }

    @Override
    public void write(BlockPos blockPos, PacketBuffer buffer) {
        buffer.writeInt(blockPos.getX());
        buffer.writeInt(blockPos.getY());
        buffer.writeInt(blockPos.getZ());
    }

    @Override
    public BlockPos read(PacketBuffer buffer) {
        return new BlockPos(
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt()
        );
    }
}
