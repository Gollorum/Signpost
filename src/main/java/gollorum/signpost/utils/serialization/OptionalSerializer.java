package gollorum.signpost.utils.serialization;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

import java.util.Optional;

public final class OptionalSerializer<T> implements CompoundSerializable<Optional<T>> {

    public static final String key = "Value";

    public static final OptionalSerializer<net.minecraft.util.ResourceLocation> ResourceLocation = new OptionalSerializer<>(
        new CompoundSerializable<net.minecraft.util.ResourceLocation>() {
            @Override
            public void writeTo(net.minecraft.util.ResourceLocation location, CompoundNBT compound, String keyPrefix) {
                compound.putString(keyPrefix, location.toString());
            }

            @Override
            public boolean isContainedIn(CompoundNBT compound, String keyPrefix) {
                return compound.contains(keyPrefix);
            }

            @Override
            public net.minecraft.util.ResourceLocation read(CompoundNBT compound, String keyPrefix) {
                return new net.minecraft.util.ResourceLocation(compound.getString(keyPrefix));
            }
        }
    );

    private final CompoundSerializable<T> valueSerializer;

    private OptionalSerializer(CompoundSerializable<T> valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    public static <T> OptionalSerializer<T> from(CompoundSerializable<T> valueSerializer) {
        return new OptionalSerializer<>(valueSerializer);
    }

    @Override
    public void writeTo(Optional<T> t, CompoundNBT compound, String keyPrefix) {
        compound.putBoolean(keyPrefix + "IsPresent", t.isPresent());
        t.ifPresent(value -> valueSerializer.writeTo(value, compound, keyPrefix + "Value"));
    }

    @Override
    public boolean isContainedIn(CompoundNBT compound, String keyPrefix) {
        return valueSerializer.isContainedIn(compound, keyPrefix + "Value");
    }

    @Override
    public Optional<T> read(CompoundNBT compound, String keyPrefix) {
        if(compound.getBoolean(keyPrefix + "IsPresent"))
            return Optional.ofNullable(valueSerializer.read(compound, keyPrefix + "Value"));
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
            return Optional.ofNullable(valueSerializer.readFrom(buffer));
        else return Optional.empty();
    }

}
