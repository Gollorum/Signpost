package gollorum.signpost.utils.serialization;

import net.minecraft.network.PacketBuffer;

public interface BufferSerializable<T> {

    Class<T> getTargetClass();

    void write(T t, PacketBuffer buffer);
    T read(PacketBuffer buffer);

    default OptionalBufferSerializer<T> optional() { return OptionalBufferSerializer.from(this); }
}
