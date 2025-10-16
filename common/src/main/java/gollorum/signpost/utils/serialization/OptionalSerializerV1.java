package gollorum.signpost.utils.serialization;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import java.util.Optional;

public class OptionalSerializerV1 {

    public static <T> MapCodec<Optional<T>> of(Codec<T> codec) {
        return Codec.BOOL.<Optional<T>>dispatchMap(
            "IsPresent", Optional::isPresent,
            b -> b
                ? codec.fieldOf("Value").xmap(Optional::of, Optional::get)
                : MapCodec.unit(Optional.empty())
        );
    }

}
