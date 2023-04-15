package cn.addenda.businesseasy.cache;

import java.util.concurrent.TimeUnit;

/**
 * kv-cache的包装类，用于实现KVCache的淘汰策略。
 *
 * @author addenda
 * @datetime 2023/1/17 14:49
 */
public abstract class EliminableKVCache<K, V> extends KVCacheWrapper<K, V> {
    /**
     * cache的大小
     */
    private final int capacity;

    protected EliminableKVCache(int capacity, KVCache<K, V> kvCacheDelegate) {
        super(kvCacheDelegate);
        if (capacity <= 0) {
            throw new IllegalArgumentException("缓存的大小需要 > 0！");
        }
        this.capacity = capacity;
    }

    @Override
    public void set(K k, V v) {
        if (containsKey(k)) {
            setWhenContainsKey(k, v);
            return;
        }
        setWhenNonContainsKey(k, v);
    }

    protected abstract void setWhenContainsKey(K k, V v);

    protected abstract void setWhenNonContainsKey(K k, V v);

    @Override
    public void set(K k, V v, long timeout, TimeUnit timeunit) {
        throw new UnsupportedOperationException("EliminableKVCache不支持缓存过期！");
//        if (containsKey(k)) {
//            setWhenContainsKey(k, v, timeout, timeunit);
//            return;
//        }
//        setWhenNonContainsKey(k, v, timeout, timeunit);
    }

    protected abstract void setWhenContainsKey(K k, V v, long timeout, TimeUnit timeunit);

    protected abstract void setWhenNonContainsKey(K k, V v, long timeout, TimeUnit timeunit);

    @Override
    public V get(K k) {
        if (!containsKey(k)) {
            return getWhenNonContainsKey(k);
        }
        return getWhenContainsKey(k);
    }

    protected abstract V getWhenNonContainsKey(K k);

    protected abstract V getWhenContainsKey(K k);

    @Override
    public boolean containsKey(K k) {
        boolean b = getKvCacheDelegate().containsKey(k);
        if (!b) {
            clearKeyUsage(k);
        }
        return b;
    }

    protected void clearKeyUsage(K k) {

    }

    @Override
    public int capacity() {
        return capacity;
    }

}
