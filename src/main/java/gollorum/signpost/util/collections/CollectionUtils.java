package gollorum.signpost.util.collections;

import java.util.List;
import java.util.function.Predicate;

public class CollectionUtils {

    public static <T> T find(List<T> list, Predicate<T> where) {
        for(T t: list){
            if(where.test(t)) return t;
        }
        return null;
    }

}