package gollorum.signpost.utils.serialization;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

import java.util.Optional;

public final class OptionalSerializer<T> implements CompoundSerializable<Optional<T>> {

    public static final String key = "Value";

    public static final OptionalSerializer<net.minecraft.util.ResourceLocation> ResourceLocation = new OptionalSerializer<>(
        new CompoundSerializable<net.minecraft.util.ResourceLocation>() {
            @Override
            public CompoundNBT write(net.minecraft.util.ResourceLocation location, CompoundNBT compound) {
                compound.putString("ResourceLocation", location.toString());
                return compound;
            }

            @Override
            public boolean isContainedIn(CompoundNBT compound) {
                return compound.contains("ResourceLocation");
            }

            @Override
            public net.minecraft.util.ResourceLocation read(CompoundNBT compound) {
                return new net.minecraft.util.ResourceLocation(compound.getString("ResourceLocation"));
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
    public CompoundNBT write(Optional<T> t, CompoundNBT compound) {
        compound.putBoolean("IsPresent", t.isPresent());
        t.ifPresent(value -> compound.put("Value", valueSerializer.write(value)));
        return compound;
    }

    @Override
    public boolean isContainedIn(CompoundNBT compound) {
        return compound.contains("IsPresent");
    }

    @Override
    public Optional<T> read(CompoundNBT compound) {
        if(compound.getBoolean("IsPresent"))
            return Optional.ofNullable(valueSerializer.read(compound.getCompound("Value")));
        else return Optional.empty();
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
