package gollorum.signpost.utils;

import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundNBT;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.function.BiFunction;

public final class BlockPartMetadata<T extends BlockPart> implements CompoundSerializable<T> {

    public final String identifier;
    public final TriConsumer<T, String, CompoundNBT> writeTo;
    public final BiFunction<CompoundNBT, String, T> read;

    public BlockPartMetadata(
        String identifier,
        TriConsumer<T, String, CompoundNBT> writeTo,
        BiFunction<CompoundNBT, String, T> read
    ) {
        this.identifier = identifier;
        this.writeTo = writeTo;
        this.read = read;
    }

    @Override
    public void writeTo(T t, CompoundNBT compound, String keyPrefix) {
        writeTo.accept(t, keyPrefix, compound);
    }

    @Override
    public boolean isContainedIn(CompoundNBT compound, String keyPrefix) {
        return true;
    }

    @Override
    public T read(CompoundNBT compound, String keyPrefix) {
        return read.apply(compound, keyPrefix);
    }
}
