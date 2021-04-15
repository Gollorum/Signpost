package gollorum.signpost.utils.serialization;

import net.minecraft.network.PacketBuffer;

public interface BufferSerializable<T> {

    void write(T t, PacketBuffer buffer);
    T read(PacketBuffer buffer);

}
