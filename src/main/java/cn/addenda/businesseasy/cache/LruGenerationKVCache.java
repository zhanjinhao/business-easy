package cn.addenda.businesseasy.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.concurrent.TimeUnit;

/**
 * 分代LRU缓存算法实现（MySQL淘汰页使用的LRU实现）。
 * <p>
 * 老年代用于存储低频数据。
 *
 * @author addenda
 * @datetime 2023/1/16 15:43
 */
public class LruGenerationKVCache<K, V> extends LruKVCache<K, V> {

    /**
     * 老年代cache大小
     */
    private int oldCapacity;
    /**
     * 连续两次访问低于阈值时入年轻代
     */
    private final long threshold;
    /**
     * 老年代：用于存储低频数据
     */
    private final LruKVCache<K, Entry<V>> oldGenerationKVCache;

    public LruGenerationKVCache(int newCapacity, int oldCapacity, int threshold) {
        super(newCapacity, new HashMapKVCache<>());
        this.oldCapacity = oldCapacity;
        this.threshold = threshold;
        oldGenerationKVCache = new LruKVCache<>(oldCapacity, new HashMapKVCache<>());
    }


    public LruGenerationKVCache(int newCapacity, int oldCapacity, int threshold, KVCache<K, V> kvCacheDelegate) {
        super(newCapacity, kvCacheDelegate);
        this.oldCapacity = oldCapacity;
        this.threshold = threshold;
        oldGenerationKVCache = new LruKVCache<>(oldCapacity, new HashMapKVCache<>());
    }

    public LruGenerationKVCache(int newCapacity, int oldCapacity, int threshold, LruDeque<K> lruDeque, KVCache<K, V> kvCacheDelegate) {
        super(lruDeque, newCapacity, kvCacheDelegate);
        this.oldCapacity = oldCapacity;
        this.threshold = threshold;
        oldGenerationKVCache = new LruKVCache<>(oldCapacity, new HashMapKVCache<>());
    }

    /**
     * -1：如果年轻代存在k，将其移至年轻代的头部并返回（与基础LRU实现一致）
     * <p>
     * 0：如果老年代不存在k，返回bull
     * <p>
     * 1：如果老年代存在k且满足阈值，移动至年轻代
     * <p>
     * 2：如果老年代存在k但不满足阈值，更新时间
     */
    @Override
    protected V getWhenNonContainsKey(K k) {
        Entry<V> entryV = oldGenerationKVCache.onlyGet(k);
        if (entryV == null) {
            return null;
        } else {
            long createTm = entryV.getVisitTm();
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - createTm > threshold) {
                entryV.setVisitTm(currentTimeMillis);
            } else {
                oldGenerationKVCache.delete(k);
                super.set(k, entryV.getV());
            }
            return entryV.getV();
        }
    }

    /**
     * 存cache的时候存入老年代
     */
    @Override
    protected void setWhenNonContainsKey(K k, V v) {
        oldGenerationKVCache.set(k, new Entry<>(v, System.currentTimeMillis()));
    }

    @Override
    protected void setWhenNonContainsKey(K k, V v, long timeout, TimeUnit timeunit) {
        oldGenerationKVCache.set(k, new Entry<>(v, System.currentTimeMillis()), timeout, timeunit);
    }

    @Override
    public void delete(K k) {
        oldGenerationKVCache.delete(k);
        super.delete(k);
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Entry<V> {
        private V v;
        private long visitTm;
    }

}
