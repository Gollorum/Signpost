package gollorum.signpost.utils.serialization;

import net.minecraft.network.PacketBuffer;

public interface BufferSerializable<T> {

    void writeTo(T t, PacketBuffer buffer);
    T readFrom(PacketBuffer buffer);

}
