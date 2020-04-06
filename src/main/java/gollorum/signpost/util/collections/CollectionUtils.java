package gollorum.signpost.util.collections;

import java.util.*;
import java.util.function.*;

public class CollectionUtils {

    public static <T> T find(List<T> list, Predicate<T> where) {
        for(T t: list){
            if(where.test(t)) return t;
        }
        return null;
    }

    public static <T> Collection<T> where(Collection<T> collection, Predicate<T> condition){
        List<T> ret = new ArrayList<>();
        for(T t: collection) {
            if(condition.test(t)) {
                ret.add(t);
            }
        }
        return ret;
    }

    public static <Key, Value> Map<Key, Value> mutateOr(
            Map<Key, Value> map,
            BiPredicate<Key, Value> condition,
            BiFunction<Key, Value, Value> mutation,
            BiConsumer<Key, Value> elseAction
    ){
        Map<Key, Value> ret = new HashMap<>();
        for(Map.Entry<Key, Value> entry: map.entrySet()) {
            Key key = entry.getKey();
            Value value = entry.getValue();
            if(condition.test(key, value)) {
                ret.put(key, mutation.apply(key, value));
            } else {
                elseAction.accept(key, value);
            }
        }
        return ret;
    }

}