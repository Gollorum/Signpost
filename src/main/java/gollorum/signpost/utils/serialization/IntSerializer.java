package gollorum.signpost.utils.serialization;

import net.minecraft.network.FriendlyByteBuf;

public final class IntSerializer implements BufferSerializable<Integer> {

    public static final IntSerializer instance = new IntSerializer();

    @Override
    public Class<Integer> getTargetClass() {
        return Integer.class;
    }

    @Override
    public void write(Integer integer, FriendlyByteBuf buffer) {
        buffer.writeInt(integer);
    }

    @Override
    public Integer read(FriendlyByteBuf buffer) {
        return buffer.readInt();
    }
}
