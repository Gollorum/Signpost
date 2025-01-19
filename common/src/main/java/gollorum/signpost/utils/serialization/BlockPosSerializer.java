package gollorum.signpost.utils.serialization;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class BlockPosSerializer implements CompoundSerializable<BlockPos> {

    public static final BlockPosSerializer INSTANCE = new BlockPosSerializer();

    private BlockPosSerializer(){}

    @Override
    public void encode(CompoundTag compound, BlockPos blockPos, HolderLookup.Provider provider) {
        compound.putInt("X", blockPos.getX());
        compound.putInt("Y", blockPos.getY());
        compound.putInt("Z", blockPos.getZ());
    }

    @Override
    public boolean isContainedIn(CompoundTag compound) {
        return compound.contains("X") &&
            compound.contains("Y") &&
            compound.contains("Z");
    }

    @Override
    public BlockPos decode(CompoundTag compound, HolderLookup.Provider provider) {
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
    public void encode(FriendlyByteBuf buffer, BlockPos blockPos) {
        buffer.writeInt(blockPos.getX());
        buffer.writeInt(blockPos.getY());
        buffer.writeInt(blockPos.getZ());
    }

    @Override
    public BlockPos decode(FriendlyByteBuf buffer) {
        return new BlockPos(
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt()
        );
    }
}
