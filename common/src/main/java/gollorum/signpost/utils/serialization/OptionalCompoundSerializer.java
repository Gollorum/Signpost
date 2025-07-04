package gollorum.signpost.utils.serialization;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

public final class OptionalCompoundSerializer<T> implements CompoundSerializable<Optional<T>> {

    public static final String key = "Value";

    private final CompoundSerializable<T> valueSerializer;

    private OptionalCompoundSerializer(CompoundSerializable<T> valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    public static <T> OptionalCompoundSerializer<T> from(CompoundSerializable<T> valueSerializer) {
        return new OptionalCompoundSerializer<>(valueSerializer);
    }

    @Override
    public void encode(CompoundTag compound, Optional<T> t, HolderLookup.Provider provider) {
        compound.putBoolean("IsPresent", t.isPresent());
        t.ifPresent(value -> compound.put("Value", valueSerializer.encode(value, provider)));
    }

    @Override
    public boolean isContainedIn(CompoundTag compound) {
        return compound.contains("IsPresent");
    }

    @Override
    public Optional<T> decode(CompoundTag compound, HolderLookup.Provider provider) {
        if(compound.getBooleanOr("IsPresent", false))
            return Optional.ofNullable(valueSerializer.decode(compound.getCompoundOrEmpty("Value"), provider));
        else return Optional.empty();
    }

}
