package gollorum.signpost.utils.serialization;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public interface BufferSerializable<T> extends StreamCodec<RegistryFriendlyByteBuf, T> {

    Class<T> getTargetClass();

    default OptionalBufferSerializer<T> optional() { return OptionalBufferSerializer.from(this); }

}
