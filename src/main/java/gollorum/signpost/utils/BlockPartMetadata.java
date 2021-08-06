package gollorum.signpost.utils;

import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundNBT;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class BlockPartMetadata<T extends BlockPart> implements CompoundSerializable<T> {

    public final String identifier;
    public final BiConsumer<T,CompoundNBT> writeTo;
    public final Function<CompoundNBT, T> read;
    private final Class<T> targetClass;

    public BlockPartMetadata(
        String identifier,
        BiConsumer<T, CompoundNBT> writeTo,
        Function<CompoundNBT, T> read,
        Class<T> targetClass) {
        this.identifier = identifier;
        this.writeTo = writeTo;
        this.read = read;
        this.targetClass = targetClass;
    }

    @Override
    public CompoundNBT write(T t, CompoundNBT compound) {
        writeTo.accept(t, compound);
        return compound;
    }

    @Override
    public boolean isContainedIn(CompoundNBT compound) {
        return true;
    }

    @Override
    public T read(CompoundNBT compound) {
        return read.apply(compound);
    }

    @Override
    public Class<T> getTargetClass() {
        return targetClass;
    }
}
