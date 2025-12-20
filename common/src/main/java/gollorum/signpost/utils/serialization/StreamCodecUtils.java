package gollorum.signpost.utils.serialization;

import net.minecraft.network.codec.StreamCodec;

import java.util.function.BiFunction;

public class StreamCodecUtils {

    public static <TBuf, T, S> StreamCodec<TBuf, S> mapWithBuf(StreamCodec<TBuf, T> baseCodec, BiFunction<TBuf, T, S> toMapped, BiFunction<TBuf, S, T> fromMapped) {
        return new StreamCodec<TBuf, S>() {
            @Override
            public S decode(TBuf tBuf) {
                return toMapped.apply(tBuf, baseCodec.decode(tBuf));
            }

            @Override
            public void encode(TBuf o, S s) {
                baseCodec.encode(o, fromMapped.apply(o, s));
            }
        };
    }

}
