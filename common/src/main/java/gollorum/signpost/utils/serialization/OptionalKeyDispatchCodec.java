package gollorum.signpost.utils.serialization;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.KeyDispatchCodec;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.stream.Stream;

// KeyDispatchCodec, but the key may be absent
public class OptionalKeyDispatchCodec<K, V> extends KeyDispatchCodec<K, V> {

    private final String typeKey;
    public final K defaultKey;
    private final Codec<K> keyCodec;

    public OptionalKeyDispatchCodec(
        final String typeKey, final K defaultKey,
        final Function<? super V, ? extends K> type,
        final Codec<K> keyCodec,
        final Function<? super K, ? extends MapCodec<? extends V>> codec
    ) {
        super(typeKey, keyCodec, type.andThen(DataResult::success), codec.andThen(DataResult::success));
        this.defaultKey = defaultKey;
        this.typeKey = typeKey;
        this.keyCodec = keyCodec;
    }

    @Override
    public <T> DataResult<V> decode(DynamicOps<T> ops, MapLike<T> input) {
        return super.decode(ops, extendIfNeeded(input, ops));
    }

    private <T> MapLike<T> extendIfNeeded(MapLike<T> original, DynamicOps<T> ops) {
        return original.get(typeKey) == null
            ? new ExtendedMap<>(original, ops)
            : original;
    }

    private class ExtendedMap<T> implements MapLike<T> {
        private final MapLike<T> original;
        private final DynamicOps<T> ops;
        private final T encodedDefaultKey;

        public ExtendedMap(MapLike<T> original, DynamicOps<T> ops) {
            this.original = original;
            this.ops = ops;
            encodedDefaultKey = keyCodec.encodeStart(ops, defaultKey).getOrThrow();
        }

        @Override
        public @Nullable T get(String key) {
            return key.equals(typeKey)
                ? encodedDefaultKey
                : original.get(key);
        }

        @Override
        public @Nullable T get(T key) {
            var k = ops.getStringValue(key);
            return k.isSuccess() && k.getOrThrow().equals(typeKey)
                ? encodedDefaultKey
                : original.get(key);
        }

        @Override
        public Stream<Pair<T, T>> entries() {
            return Stream.concat(Stream.of(Pair.of(ops.createString(typeKey), encodedDefaultKey)), original.entries());
        }
    }
}
