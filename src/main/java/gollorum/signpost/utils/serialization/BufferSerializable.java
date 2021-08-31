package gollorum.signpost.utils.serialization;

import net.minecraft.network.FriendlyByteBuf;

public interface BufferSerializable<T> {

    Class<T> getTargetClass();

    void write(T t, FriendlyByteBuf buffer);
    T read(FriendlyByteBuf buffer);

    default OptionalBufferSerializer<T> optional() { return OptionalBufferSerializer.from(this); }
}
