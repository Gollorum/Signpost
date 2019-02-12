package gollorum.signpost.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import gollorum.signpost.util.code.MinecraftIndependent;

@MinecraftIndependent
public abstract class BiomePaintHandler implements Map<String, BiomePaintEntry> {

	protected HashMap<String, BiomePaintEntry> biomeList;

	public static final BiomePaintHandler DEFAULT_HANDLER = new BiomePaintHandler() {
		{
			biomeList = new HashMap<String, BiomePaintEntry>();
			put(BiomeLibrary.getInstance().getName("plains"), BiomePaintEntry.DEFAULT_PLAINS);
			put(BiomeLibrary.getInstance().getName("desert"), BiomePaintEntry.DEFAULT_DESERT);
			put(BiomeLibrary.getInstance().getName("savanna"), BiomePaintEntry.DEFAULT_SAVANNA);
			put(BiomeLibrary.getInstance().getName("taiga"), BiomePaintEntry.DEFAULT_TAIGA);
		}
	};

	@Override
	public int size() {
		return biomeList.size();
	}

	@Override
	public boolean isEmpty() {
		return biomeList.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return biomeList.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return biomeList.containsValue(value);
	}

	@Override
	public BiomePaintEntry get(Object key) {
		BiomePaintEntry ret = biomeList.get(key);
		if (ret == null) {
			return (BiomePaintEntry) values().toArray()[0];
		} else {
			return ret;
		}
	}

	@Override
	public BiomePaintEntry put(String key, BiomePaintEntry value) {
		return biomeList.put(key, value);
	}

	@Override
	public BiomePaintEntry remove(Object key) {
		return biomeList.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends BiomePaintEntry> m) {
		biomeList.putAll(m);
	}

	@Override
	public void clear() {
		biomeList.clear();
	}

	@Override
	public Set<String> keySet() {
		return biomeList.keySet();
	}

	@Override
	public Collection<BiomePaintEntry> values() {
		return biomeList.values();
	}

	@Override
	public Set<java.util.Map.Entry<String, BiomePaintEntry>> entrySet() {
		return biomeList.entrySet();
	}

}