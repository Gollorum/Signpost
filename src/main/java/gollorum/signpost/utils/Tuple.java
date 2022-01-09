package gollorum.signpost.utils;

import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Tuple<T1, T2> {

    public final T1 _1;
    public final T2 _2;

    public Tuple(T1 _1, T2 _2) {
        this._1 = _1;
        this._2 = _2;
    }

    public static <T1, T2> Tuple<T1, T2> from(org.apache.commons.lang3.tuple.Pair<T1, T2> pair) {
        return new Tuple<>(pair.getLeft(), pair.getRight());
    }

    public static <T1, T2> Tuple<T1, T2> from(Map.Entry<T1, T2> pair) {
        return new Tuple<>(pair.getKey(), pair.getValue());
    }

    public T1 getLeft() { return _1; }
    public T2 getRight() { return _2; }

    public Tuple<T2, T1> flip() { return new Tuple<>(_2, _1); }

    public static <T1, T2> Tuple<T1, T2> of(T1 left, T2 right) { return new Tuple<>(left, right); }
    public static <T1, T2, T3> Tuple<Tuple<T1, T2>, T3> of(T1 left, T2 right, T3 last) { return Tuple.of(Tuple.of(left, right), last); }

    public static <Key, Value> Collector<Tuple<Key, Value>, ?, Map<Key, Value>> mapCollector() {
        return Collectors.toMap(t -> t._1, t -> t._2);
    }

    public static class Serializer<T1, T2> implements CompoundSerializable<Tuple<T1, T2>> {

        private final CompoundSerializable<T1> serializer1;
        private final CompoundSerializable<T2> serializer2;

        public Serializer(CompoundSerializable<T1> serializer1, CompoundSerializable<T2> serializer2) {
            this.serializer1 = serializer1;
            this.serializer2 = serializer2;
        }

        @Override
        public Class<Tuple<T1, T2>> getTargetClass() {
            return (Class<Tuple<T1, T2>>) new Tuple<T1, T2>(null, null).getClass();
        }

        @Override
        public CompoundNBT write(Tuple<T1, T2> tuple, CompoundNBT compound) {
            compound.put("left", serializer1.write(tuple._1));
            compound.put("right", serializer2.write(tuple._2));
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundNBT compound) {
            return compound.contains("left")
                && compound.contains("right")
                && serializer1.isContainedIn(compound.getCompound("left"))
                && serializer2.isContainedIn(compound.getCompound("right"));
        }

        @Override
        public Tuple<T1, T2> read(CompoundNBT compound) {
            return new Tuple<>(
                serializer1.read(compound.getCompound("left")),
                serializer2.read(compound.getCompound("right"))
            );
        }

        @Override
        public void write(Tuple<T1, T2> tuple, PacketBuffer buffer) {
            serializer1.write(tuple._1, buffer);
            serializer2.write(tuple._2, buffer);
        }

        @Override
        public Tuple<T1, T2> read(PacketBuffer buffer) {
            return new Tuple<>(
                serializer1.read(buffer),
                serializer2.read(buffer)
            );
        }
    }
}
