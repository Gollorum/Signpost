package gollorum.signpost.utils.serialization;

import net.minecraft.network.PacketBuffer;

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
    public void write(Optional<T> t, PacketBuffer buffer) {
        if(t.isPresent()) {
            buffer.writeBoolean(true);
            valueSerializer.write(t.get(), buffer);
        } else buffer.writeBoolean(false);
    }

    @Override
    public Optional<T> read(PacketBuffer buffer) {
        if(buffer.readBoolean())
            return Optional.ofNullable(valueSerializer.read(buffer));
        else return Optional.empty();
    }

}
