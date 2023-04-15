package cn.addenda.businesseasy.cache;

import java.util.concurrent.TimeUnit;

/**
 * LRU-K缓存算法实现（基础的LRU缓存等价于LRU-1）。<p/>
 * LRU-1通过LeetCode测似。
 *
 * @author addenda
 * @datetime 2023/1/16 15:43
 */
public class LruKKVCache<K, V> extends LruKVCache<K, V> {
    /**
     * 访问多少次可以入队
     */
    private final int threshold;
    /**
     * 记录key被访问了多少次
     */
    private final KVCache<K, Integer> visitorCount;

    public LruKKVCache(int capacity, int threshold) {
        super(capacity, new HashMapKVCache<>());
        this.threshold = threshold;
        this.visitorCount = new LruKVCache<>(capacity * 2, new HashMapKVCache<>());
    }

    public LruKKVCache(int capacity, int threshold,
                       KVCache<K, V> kvCacheDelegate) {
        super(capacity, kvCacheDelegate);
        if (kvCacheDelegate instanceof EliminableKVCache) {
            throw new IllegalArgumentException("LruKKVCache的kvCacheDelegate不能是EliminableKVCache，当前是：" + kvCacheDelegate.getClass() + "。");
        }
        this.threshold = threshold;
        this.visitorCount = new LruKVCache<>(capacity * 2, new HashMapKVCache<>());
    }

    public LruKKVCache(int capacity, int threshold,
                       LruDeque<K> lruDeque, KVCache<K, V> kvCacheDelegate) {
        super(lruDeque, capacity, kvCacheDelegate);
        if (kvCacheDelegate instanceof EliminableKVCache) {
            throw new IllegalArgumentException("LruKKVCache的kvCacheDelegate不能是EliminableKVCache，当前是：" + kvCacheDelegate.getClass() + "。");
        }
        this.threshold = threshold;
        this.visitorCount = new LruKVCache<>(capacity * 2, new HashMapKVCache<>());
    }

    public LruKKVCache(int capacity, int threshold,
                       LruKVCache<K, Integer> visitorCount, KVCache<K, V> kvCacheDelegate) {
        super(capacity, kvCacheDelegate);
        if (kvCacheDelegate instanceof EliminableKVCache) {
            throw new IllegalArgumentException("LruKKVCache的kvCacheDelegate不能是EliminableKVCache，当前是：" + kvCacheDelegate.getClass() + "。");
        }
        this.threshold = threshold;
        this.visitorCount = visitorCount;
    }

    public LruKKVCache(int capacity, int threshold,
                       LruKVCache<K, Integer> visitorCount, LruDeque<K> lruDeque, KVCache<K, V> kvCacheDelegate) {
        super(lruDeque, capacity, kvCacheDelegate);
        if (kvCacheDelegate instanceof EliminableKVCache) {
            throw new IllegalArgumentException("LruKKVCache的kvCacheDelegate不能是EliminableKVCache，当前是：" + kvCacheDelegate.getClass() + "。");
        }
        this.threshold = threshold;
        this.visitorCount = visitorCount;
    }

    @Override
    public V get(K k) {
        Integer count = visitorCount.get(k);
        visitorCount.set(k, count == null ? 1 : count + 1);
        return super.get(k);
    }

    @Override
    protected void setWhenNonContainsKey(K k, V v) {
        // 如果不到入队次数，不入队
        Integer count = visitorCount.computeIfAbsent(k, s -> 1);
        if (count >= threshold) {
            visitorCount.delete(k);
            super.setWhenNonContainsKey(k, v);
        }
    }

    @Override
    protected void setWhenNonContainsKey(K k, V v, long timeout, TimeUnit timeunit) {
        // 如果不到入队次数，不入队
        Integer count = visitorCount.computeIfAbsent(k, s -> 1);
        if (count >= threshold) {
            visitorCount.delete(k);
            super.setWhenNonContainsKey(k, v, timeout, timeunit);
        }
    }

    /**
     * 自动过期的需要清理visitorCount
     * <p>
     * 本来就不存在的不需要清理visitorCount
     */
    @Override
    protected void clearKeyUsage(K k) {
        // lruDeque里如果存在k，说明k是自动过期的
        if (getLruDeque().contains(k)) {
            visitorCount.delete(k);
        }
        super.clearKeyUsage(k);
    }

    @Override
    public void delete(K k) {
        super.delete(k);
        visitorCount.delete(k);
    }

}
