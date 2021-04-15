package gollorum.signpost.utils;

import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundNBT;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class BlockPartMetadata<T extends BlockPart> implements CompoundSerializable<T> {

    public final String identifier;
    public final BiConsumer<T,CompoundNBT> writeTo;
    public final Function<CompoundNBT, T> read;

    public BlockPartMetadata(
        String identifier,
        BiConsumer<T, CompoundNBT> writeTo,
        Function<CompoundNBT, T> read
    ) {
        this.identifier = identifier;
        this.writeTo = writeTo;
        this.read = read;
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
}
