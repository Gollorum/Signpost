package gollorum.signpost.utils.serialization;

import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.Optional;

public class OptionalBufferSerializer<T> implements BufferSerializable<Optional<T>> {

    private final BufferSerializable<T> valueSerializer;

    public static <T> OptionalBufferSerializer<T> from(BufferSerializable<T> valueSerializer) {
        return new OptionalBufferSerializer<>(valueSerializer);
    }

    public OptionalBufferSerializer(BufferSerializable<T> valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    @Override
    public Class<Optional<T>> getTargetClass() {
        return (Class<Optional<T>>) Optional.<T>empty().getClass();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, Optional<T> t) {
        if(t.isPresent()) {
            buffer.writeBoolean(true);
            valueSerializer.encode(buffer, t.get());
        } else buffer.writeBoolean(false);
    }

    @Override
    public Optional<T> decode(RegistryFriendlyByteBuf buffer) {
        if(buffer.readBoolean())
            return Optional.ofNullable(valueSerializer.decode(buffer));
        else return Optional.empty();
    }

}
