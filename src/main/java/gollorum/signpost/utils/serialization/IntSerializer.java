package gollorum.signpost.utils.serialization;

import net.minecraft.network.PacketBuffer;

public final class IntSerializer implements BufferSerializable<Integer> {
    @Override
    public Class<Integer> getTargetClass() {
        return Integer.class;
    }

    @Override
    public void write(Integer integer, PacketBuffer buffer) {
        buffer.writeInt(integer);
    }

    @Override
    public Integer read(PacketBuffer buffer) {
        return buffer.readInt();
    }
}
