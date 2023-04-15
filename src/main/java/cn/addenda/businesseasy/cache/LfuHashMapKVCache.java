package cn.addenda.businesseasy.cache;

import java.util.LinkedHashSet;

/**
 * @author addenda
 * @datetime 2023/4/15 12:45
 */
public class LfuHashMapKVCache<K, V> extends LfuKVCache<K, V> {

    public LfuHashMapKVCache(int capacity) {
        super(capacity, new HashMapKVCache<>());
    }

    public LfuHashMapKVCache(KVCache<K, Integer> keyToVisitorCount, SortedKVCache<Integer, LinkedHashSet<K>> visitorCountToKeySet, int capacity) {
        super(keyToVisitorCount, visitorCountToKeySet, capacity, new HashMapKVCache<>());
    }

}
