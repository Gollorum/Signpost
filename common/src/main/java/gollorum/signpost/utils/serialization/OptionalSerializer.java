package gollorum.signpost.utils.serialization;

import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

public final class OptionalSerializer<T> extends OptionalBufferSerializer<T> implements CompoundSerializable<Optional<T>> {

    public static final String key = "Value";

    private final CompoundSerializable<T> valueSerializer;

    private OptionalSerializer(CompoundSerializable<T> valueSerializer) {
        super(valueSerializer);
        this.valueSerializer = valueSerializer;
    }

    public static <T> OptionalSerializer<T> from(CompoundSerializable<T> valueSerializer) {
        return new OptionalSerializer<>(valueSerializer);
    }

    @Override
    public CompoundTag write(Optional<T> t, CompoundTag compound) {
        compound.putBoolean("IsPresent", t.isPresent());
        t.ifPresent(value -> compound.put("Value", valueSerializer.write(value)));
        return compound;
    }

    @Override
    public boolean isContainedIn(CompoundTag compound) {
        return compound.contains("IsPresent");
    }

    @Override
    public Optional<T> read(CompoundTag compound) {
        if(compound.getBoolean("IsPresent"))
            return Optional.ofNullable(valueSerializer.read(compound.getCompound("Value")));
        else return Optional.empty();
    }

}
