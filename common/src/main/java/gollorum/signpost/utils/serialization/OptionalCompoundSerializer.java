package gollorum.signpost.utils.serialization;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public final class OptionalCompoundSerializer {

    public static final String key = "Value";

    public static <T> Codec<Optional<T>> from(Codec<T> inner) {
        return RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("IsPresent").forGetter(Optional::isPresent),
            inner.optionalFieldOf(key).forGetter(optional -> optional)
        ).apply(i, (isPresent, value) -> isPresent ? value : Optional.empty()));
    }

    public static <T> Codec<Optional<T>> from(MapCodec<Optional<T>> inner) {
        return RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("IsPresent").forGetter(Optional::isPresent),
            inner.forGetter(optional -> optional)
        ).apply(i, (isPresent, value) -> isPresent ? value : Optional.empty()));
    }

}
