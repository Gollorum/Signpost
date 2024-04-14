package gollorum.signpost.utils;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.Collection;
import java.util.Map;

public class CollectionUtils {

	public static <T> Map<T, Integer> group(Collection<T> in) {
		Object2IntMap<T> map = new Object2IntLinkedOpenHashMap<>();
		for(T t : in) map.computeInt(t, (x, i) -> i == null ? 1 : i + 1);
		return map;
	}

}
