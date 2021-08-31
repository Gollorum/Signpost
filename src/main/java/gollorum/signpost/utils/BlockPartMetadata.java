package gollorum.signpost.utils;

import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundTag;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class BlockPartMetadata<T extends BlockPart> implements CompoundSerializable<T> {

    public final String identifier;
    public final BiConsumer<T,CompoundTag> writeTo;
    public final Function<CompoundTag, T> read;
    private final Class<T> targetClass;

    public BlockPartMetadata(
        String identifier,
        BiConsumer<T, CompoundTag> writeTo,
        Function<CompoundTag, T> read,
        Class<T> targetClass) {
        this.identifier = identifier;
        this.writeTo = writeTo;
        this.read = read;
        this.targetClass = targetClass;
    }

    @Override
    public CompoundTag write(T t, CompoundTag compound) {
        writeTo.accept(t, compound);
        return compound;
    }

    @Override
    public boolean isContainedIn(CompoundTag compound) {
        return true;
    }

    @Override
    public T read(CompoundTag compound) {
        return read.apply(compound);
    }

    @Override
    public Class<T> getTargetClass() {
        return targetClass;
    }
}
