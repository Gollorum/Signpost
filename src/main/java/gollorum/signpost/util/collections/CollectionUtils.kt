package gollorum.signpost.util.collections

import java.util.function.Predicate

class CollectionUtils {

    companion object {
        fun <T>find(list : List<T>, where : Predicate<T>) : T? {
            for(t in list){
                if(where.test(t)) return t;
            }
            return null;
        }
    }

}