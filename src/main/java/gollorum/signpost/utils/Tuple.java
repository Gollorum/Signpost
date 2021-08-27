package gollorum.signpost.utils;

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

    public T1 getLeft() { return _1; }
    public T2 getRight() { return _2; }

    public Tuple<T2, T1> flip() { return new Tuple<>(_2, _1); }

    public static <T1, T2> Tuple<T1, T2> of(T1 left, T2 right) { return new Tuple<>(left, right); }

    public static <Key, Value> Collector<Tuple<Key, Value>, ?, Map<Key, Value>> mapCollector() {
        return Collectors.toMap(t -> t._1, t -> t._2);
    }
}
