package cn.addenda.businesseasy.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于HashMap实现的KVOperator
 *
 * @author addenda
 * @datetime 2023/1/16 19:18
 */
public class HashMapKVCache<K, V> implements KVCache<K, V> {

    private final Map<K, V> map = new HashMap<>();

    @Override
    public void set(K k, V v) {
        map.put(k, v);
    }

    @Override
    public void set(K k, V v, long timeout, TimeUnit timeunit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(K k) {
        return map.containsKey(k);
    }

    @Override
    public V get(K k) {
        return map.get(k);
    }

    @Override
    public void delete(K k) {
        map.remove(k);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public int capacity() {
        return Integer.MAX_VALUE;
    }

}
