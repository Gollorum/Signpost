package gollorum.signpost.utils;

import net.minecraft.network.codec.StreamCodec;

import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public record Tuple<T1, T2>(T1 _1, T2 _2) {

    public static <T1, T2> Tuple<T1, T2> from(org.apache.commons.lang3.tuple.Pair<T1, T2> pair) {
        return new Tuple<>(pair.getLeft(), pair.getRight());
    }

    public static <T1, T2> Tuple<T1, T2> from(Map.Entry<T1, T2> pair) {
        return new Tuple<>(pair.getKey(), pair.getValue());
    }

    public T1 getLeft() {
        return _1;
    }

    public T2 getRight() {
        return _2;
    }

    public Tuple<T2, T1> flip() {
        return new Tuple<>(_2, _1);
    }

    public static <T1, T2> Tuple<T1, T2> of(T1 left, T2 right) {
        return new Tuple<>(left, right);
    }

    public static <T1, T2, T3> Tuple<Tuple<T1, T2>, T3> of(T1 left, T2 right, T3 last) {
        return Tuple.of(Tuple.of(left, right), last);
    }

    public static <Key, Value> Collector<Tuple<Key, Value>, ?, Map<Key, Value>> mapCollector() {
        return Collectors.toMap(t -> t._1, t -> t._2);
    }

    public static <TBuf, T1, T2> StreamCodec<TBuf, Tuple<T1, T2>> streamCodec(StreamCodec<? super TBuf, T1> codec1, StreamCodec<? super TBuf, T2> codec2) {
        return new StreamCodec<>() {
            @Override
            public void encode(TBuf buffer, Tuple<T1, T2> tuple) {
                codec1.encode(buffer, tuple._1);
                codec2.encode(buffer, tuple._2);
            }

            @Override
            public Tuple<T1, T2> decode(TBuf buffer) {
                return new Tuple<>(codec1.decode(buffer), codec2.decode(buffer));
            }
        };
    }

    //    public static class Serializer<T1, T2> implements CompoundSerializable<Tuple<T1, T2>> {
//
//        private final CompoundSerializable<T1> serializer1;
//        private final CompoundSerializable<T2> serializer2;
//
//        public Serializer(CompoundSerializable<T1> serializer1, CompoundSerializable<T2> serializer2) {
//            this.serializer1 = serializer1;
//            this.serializer2 = serializer2;
//        }
//
//        @Override
//        public Class<Tuple<T1, T2>> getTargetClass() {
//            return (Class<Tuple<T1, T2>>) new Tuple<T1, T2>(null, null).getClass();
//        }
//
//        @Override
//        public void encode(CompoundTag compound, Tuple<T1, T2> tuple, HolderLookup.Provider provider) {
//            compound.put("left", serializer1.encode(tuple._1, ));
//            compound.put("right", serializer2.encode(tuple._2, ));
//            return compound;
//        }
//
//        @Override
//        public boolean isContainedIn(CompoundTag compound) {
//            return compound.contains("left")
//                && compound.contains("right")
//                && serializer1.isContainedIn(compound.getCompound("left"))
//                && serializer2.isContainedIn(compound.getCompound("right"));
//        }
//
//        @Override
//        public Tuple<T1, T2> decode(CompoundTag compound, HolderLookup.Provider provider) {
//            return new Tuple<>(
//                serializer1.decode(compound.getCompound("left"), ),
//                serializer2.decode(compound.getCompound("right"), )
//            );
//        }
//
//        @Override
//        public void encode(FriendlyByteBuf buffer, Tuple<T1, T2> tuple) {
//            serializer1.encode(buffer, tuple._1, );
//            serializer2.encode(buffer, tuple._2, );
//        }
//
//        @Override
//        public Tuple<T1, T2> decode(FriendlyByteBuf buffer) {
//            return new Tuple<>(
//                serializer1.decode(buffer, ),
//                serializer2.decode(buffer, )
//            );
//        }
//    }
}
