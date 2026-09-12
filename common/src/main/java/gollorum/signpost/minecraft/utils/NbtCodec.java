package gollorum.signpost.minecraft.utils;

import com.mojang.serialization.MapCodec;
import gollorum.signpost.Signpost;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.util.Optional;

/**
 * 1.21.1 block entities read and write a raw {@link CompoundTag} - there is no {@code ValueInput} /
 * {@code ValueOutput} yet - so the {@link MapCodec}s that describe the persisted data have to be
 * applied to the tag by hand. These two mirror {@code ValueOutput.store} and {@code ValueInput.read}:
 * the codec's fields are merged into, and read back from, the root of the tag.
 */
public final class NbtCodec {

    private NbtCodec() {}

    public static <T> void store(CompoundTag tag, HolderLookup.Provider registries, MapCodec<T> codec, T value) {
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        codec.codec().encodeStart(ops, value)
            .resultOrPartial(error -> Signpost.LOGGER.error("Failed to write nbt: " + error))
            .ifPresent(encoded -> {
                if(encoded instanceof CompoundTag compound) tag.merge(compound);
                else Signpost.LOGGER.error("Failed to write nbt: expected a compound but got " + encoded);
            });
    }

    public static <T> Optional<T> read(CompoundTag tag, HolderLookup.Provider registries, MapCodec<T> codec) {
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        return codec.codec().parse(ops, (Tag) tag)
            .resultOrPartial(error -> Signpost.LOGGER.error("Failed to read nbt: " + error));
    }
}
