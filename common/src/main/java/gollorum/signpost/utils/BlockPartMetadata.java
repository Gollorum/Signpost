package gollorum.signpost.utils;

import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import java.util.function.BiFunction;

public final class BlockPartMetadata<T extends BlockPart> implements CompoundSerializable<T> {

    public interface Serializer<T> {
        void accept(T t, CompoundTag compound, HolderLookup.Provider provider);
    }

    public final String identifier;
    public final Serializer<T> writeTo;
    public final BiFunction<CompoundTag, HolderLookup.Provider, T> read;
    private final Class<T> targetClass;

    public BlockPartMetadata(
        String identifier,
        Serializer<T> writeTo,
        BiFunction<CompoundTag, HolderLookup.Provider, T> read,
        Class<T> targetClass) {
        this.identifier = identifier;
        this.writeTo = writeTo;
        this.read = read;
        this.targetClass = targetClass;
    }

    @Override
    public void encode(CompoundTag compound, T t, HolderLookup.Provider provider) {
        writeTo.accept(t, compound, provider);
    }

    @Override
    public boolean isContainedIn(CompoundTag compound) {
        return true;
    }

    @Override
    public T decode(CompoundTag compound, HolderLookup.Provider provider) {
        return read.apply(compound, provider);
    }
}
