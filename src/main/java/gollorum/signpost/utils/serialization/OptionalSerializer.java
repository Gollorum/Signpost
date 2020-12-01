package gollorum.signpost.utils.serialization;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.network.PacketBuffer;

import java.util.Optional;

public final class OptionalSerializer<T> implements CompoundSerializable<Optional<T>> {

    public static final String key = "Value";

    public static final OptionalSerializer<java.util.UUID> UUID = new OptionalSerializer<>(
        new CompoundSerializable<java.util.UUID>() {
            @Override
            public void writeTo(java.util.UUID uuid, CompoundNBT compound, String keyPrefix) {
                compound.putUniqueId(keyPrefix, uuid);
            }

            @Override
            public boolean isContainedIn(CompoundNBT compound, String keyPrefix) {
                return compound.hasUniqueId(keyPrefix);
            }

            @Override
            public java.util.UUID read(CompoundNBT compound, String keyPrefix) {
                return compound.getUniqueId(keyPrefix);
            }
        });

    public static final OptionalSerializer<ItemStack> ItemStack = new OptionalSerializer<>(
        new CompoundSerializable<net.minecraft.item.ItemStack>() {
            @Override
            public void writeTo(net.minecraft.item.ItemStack itemStack, CompoundNBT compound, String keyPrefix) {
                compound.put(keyPrefix, itemStack.write(new CompoundNBT()));
            }

            @Override
            public boolean isContainedIn(CompoundNBT compound, String keyPrefix) {
                return compound.contains(keyPrefix);
            }

            @Override
            public net.minecraft.item.ItemStack read(CompoundNBT compound, String keyPrefix) {
                INBT readCompound = compound.get(keyPrefix);
                if(readCompound instanceof CompoundNBT)
                    return net.minecraft.item.ItemStack.read((CompoundNBT) readCompound);
                else return null;
            }
        }
    );

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
