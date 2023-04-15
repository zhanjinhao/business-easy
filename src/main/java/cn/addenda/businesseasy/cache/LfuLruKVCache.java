package cn.addenda.businesseasy.cache;

import java.util.LinkedHashSet;

/**
 * @author addenda
 * @datetime 2022/12/28 20:08
 */
public class LfuLruKVCache<K, V> extends LfuKVCache<K, V> {

    public LfuLruKVCache(int capacity) {
        super(capacity, new LruKVCache<>(capacity, new HashMapKVCache<>()));
    }

    public LfuLruKVCache(int capacity, LruKVCache<K, V> kvCacheDelegate) {
        super(capacity, kvCacheDelegate);
    }

    public LfuLruKVCache(KVCache<K, Integer> keyToVisitorCount,
                         SortedKVCache<Integer, LinkedHashSet<K>> visitorCountToKeySet,
                         int capacity, LruKVCache<K, V> kvCacheDelegate) {
        super(keyToVisitorCount, visitorCountToKeySet, capacity, kvCacheDelegate);
    }

}
