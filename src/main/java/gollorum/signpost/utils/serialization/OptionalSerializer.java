package gollorum.signpost.utils.serialization;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

import java.util.Optional;

public final class OptionalSerializer<T> implements CompoundSerializable<Optional<T>> {

    private final CompoundSerializable<T> valueSerializer;

    public OptionalSerializer(CompoundSerializable<T> valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    @Override
    public void writeTo(Optional<T> t, CompoundNBT compound, String keyPrefix) {
        compound.putBoolean(keyPrefix + "IsPresent", t.isPresent());
        t.ifPresent(value -> valueSerializer.writeTo(value, compound, keyPrefix + "Value"));
    }

    @Override
    public Optional<T> read(CompoundNBT compound, String keyPrefix) {
        if(compound.getBoolean(keyPrefix + "IsPresent"))
            return Optional.of(valueSerializer.read(compound, keyPrefix + "Value"));
        else return Optional.empty();
    }

    @Override
    public void writeTo(Optional<T> t, PacketBuffer buffer) {
        if(t.isPresent()) {
            buffer.writeBoolean(true);
            valueSerializer.writeTo(t.get(), buffer);
        } else buffer.writeBoolean(false);
    }

    @Override
    public Optional<T> readFrom(PacketBuffer buffer) {
        if(buffer.readBoolean())
            return Optional.of(valueSerializer.readFrom(buffer));
        else return Optional.empty();
    }

}
