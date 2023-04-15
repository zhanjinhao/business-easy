package cn.addenda.businesseasy.cache;

import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * @author addenda
 * @datetime 2023/1/18 9:31
 */
public abstract class SortedMapKVCache<K, V> implements SortedKVCache<K, V> {

    private final SortedMap<K, V> sortedMap;

    protected SortedMapKVCache(SortedMap<K, V> sortedMap) {
        this.sortedMap = sortedMap;
    }

    @Override
    public void set(K k, V v) {
        sortedMap.put(k, v);
    }

    @Override
    public void set(K k, V v, long timeout, TimeUnit timeunit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(K k) {
        return sortedMap.containsKey(k);
    }

    @Override
    public V get(K k) {
        return sortedMap.get(k);
    }

    @Override
    public void delete(K k) {
        sortedMap.remove(k);
    }

    @Override
    public int size() {
        return sortedMap.size();
    }

    @Override
    public int capacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public K getFirst() {
        return sortedMap.firstKey();
    }

    @Override
    public K getLast() {
        return sortedMap.lastKey();
    }

    @Override
    public Set<K> keySet() {
        return sortedMap.keySet();
    }

}
