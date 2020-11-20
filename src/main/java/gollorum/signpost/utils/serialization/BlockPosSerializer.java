package gollorum.signpost.utils.serialization;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

public class BlockPosSerializer implements CompoundSerializable<BlockPos> {

    public static final BlockPosSerializer INSTANCE = new BlockPosSerializer();

    private BlockPosSerializer(){}

    @Override
    public void writeTo(BlockPos blockPos, CompoundNBT compound, String keyPrefix) {
        compound.putInt(keyPrefix + "X", blockPos.getX());
        compound.putInt(keyPrefix + "Y", blockPos.getY());
        compound.putInt(keyPrefix + "Z", blockPos.getZ());
    }

    @Override
    public BlockPos read(CompoundNBT compound, String keyPrefix) {
        return new BlockPos(
            compound.getInt(keyPrefix + "X"),
            compound.getInt(keyPrefix + "Y"),
            compound.getInt(keyPrefix + "Z")
        );
    }

    @Override
    public void writeTo(BlockPos blockPos, PacketBuffer buffer) {
        buffer.writeInt(blockPos.getX());
        buffer.writeInt(blockPos.getY());
        buffer.writeInt(blockPos.getZ());
    }

    @Override
    public BlockPos readFrom(PacketBuffer buffer) {
        return new BlockPos(
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt()
        );
    }
}
