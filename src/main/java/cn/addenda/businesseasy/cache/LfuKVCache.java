package cn.addenda.businesseasy.cache;

import java.util.LinkedHashSet;
import java.util.concurrent.TimeUnit;

/**
 * LFU缓存：基础的LFU缓存，基于双哈希表构建。
 *
 * @author addenda
 * @datetime 2022/12/28 20:08
 */
public class LfuKVCache<K, V> extends EliminableKVCache<K, V> {
    /**
     * 记录key出现的次数
     */
    private final KVCache<K, Integer> keyToVisitorCount;
    /**
     * 记录次数队列的keys
     */
    private final SortedKVCache<Integer, LinkedHashSet<K>> visitorCountToKeySet;

    public LfuKVCache(int capacity, KVCache<K, V> kvCacheDelegate) {
        super(capacity, kvCacheDelegate);
        this.keyToVisitorCount = new HashMapKVCache<>();
        this.visitorCountToKeySet = new TreeMapKVCache<>();
    }

    public LfuKVCache(KVCache<K, Integer> keyToVisitorCount,
                      SortedKVCache<Integer, LinkedHashSet<K>> visitorCountToKeySet,
                      int capacity, KVCache<K, V> kvCacheDelegate) {
        super(capacity, kvCacheDelegate);
        this.keyToVisitorCount = keyToVisitorCount;
        this.visitorCountToKeySet = visitorCountToKeySet;
    }

    @Override
    protected void setWhenContainsKey(K k, V v) {
        calculateKeyUsage(k);
        getKvCacheDelegate().set(k, v);
    }

    @Override
    protected void setWhenNonContainsKey(K k, V v) {
        if (capacity() == keyToVisitorCount.size()) {
            removeMinOldest();
        }

        keyToVisitorCount.set(k, 0);
        visitorCountToKeySet.computeIfAbsent(0, s -> new LinkedHashSet<>()).add(k);
        getKvCacheDelegate().set(k, v);
    }

    @Override
    protected void setWhenContainsKey(K k, V v, long timeout, TimeUnit timeunit) {
        calculateKeyUsage(k);
        getKvCacheDelegate().set(k, v, timeout, timeunit);
    }

    @Override
    protected void setWhenNonContainsKey(K k, V v, long timeout, TimeUnit timeunit) {
        if (capacity() == keyToVisitorCount.size()) {
            removeMinOldest();
        }

        keyToVisitorCount.set(k, 0);
        visitorCountToKeySet.computeIfAbsent(0, s -> new LinkedHashSet<>()).add(k);
        getKvCacheDelegate().set(k, v, timeout, timeunit);
    }

    private void removeMinOldest() {
        Integer minVisitorCount =
                visitorCountToKeySet.size() == 0 ? 0 : visitorCountToKeySet.getFirst();

        LinkedHashSet<K> keyList = visitorCountToKeySet.get(minVisitorCount);
        K oldest = keyList.iterator().next();
        keyList.remove(oldest);
        if (keyList.isEmpty()) {
            visitorCountToKeySet.delete(minVisitorCount);
        }

        keyToVisitorCount.delete(oldest);
        getKvCacheDelegate().delete(oldest);
    }

    @Override
    protected V getWhenNonContainsKey(K k) {
        return null;
    }

    @Override
    protected V getWhenContainsKey(K k) {
        calculateKeyUsage(k);
        return getKvCacheDelegate().get(k);
    }


    private void calculateKeyUsage(K k) {
        Integer visitorCount = keyToVisitorCount.get(k);
        keyToVisitorCount.set(k, visitorCount + 1);

        LinkedHashSet<K> keyList = visitorCountToKeySet.get(visitorCount);
        keyList.remove(k);
        if (keyList.isEmpty()) {
            visitorCountToKeySet.delete(visitorCount);
        }

        visitorCountToKeySet.computeIfAbsent(visitorCount + 1, s -> new LinkedHashSet<>()).add(k);
    }

    @Override
    protected void clearKeyUsage(K k) {
        Integer visitorCount = keyToVisitorCount.get(k);
        keyToVisitorCount.delete(k);

        LinkedHashSet<K> keyList = visitorCountToKeySet.get(visitorCount);
        keyList.remove(k);
        if (keyList.isEmpty()) {
            visitorCountToKeySet.delete(visitorCount);
        }
    }

    @Override
    public void delete(K k) {
        clearKeyUsage(k);

        getKvCacheDelegate().delete(k);
    }

    protected KVCache<K, Integer> getKeyToVisitorCount() {
        return keyToVisitorCount;
    }

    protected SortedKVCache<Integer, LinkedHashSet<K>> getVisitorCountToKeySet() {
        return visitorCountToKeySet;
    }
}
