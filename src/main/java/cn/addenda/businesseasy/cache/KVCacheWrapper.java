package cn.addenda.businesseasy.cache;

/**
 * kv-cache包装类的抽象。
 *
 * @author addenda
 * @datetime 2023/1/18 17:17
 */
public abstract class KVCacheWrapper<K, V> implements KVCache<K, V> {
    /**
     * 真正存储数据的cache
     */
    private final KVCache<K, V> kvCacheDelegate;

    protected KVCacheWrapper(KVCache<K, V> kvCacheDelegate) {
        if (kvCacheDelegate == null) {
            throw new IllegalArgumentException("kvCacheDelegate is null! ");
        }
        this.kvCacheDelegate = kvCacheDelegate;
    }

    @Override
    public int size() {
        return kvCacheDelegate.size();
    }

    protected KVCache<K, V> getKvCacheDelegate() {
        return kvCacheDelegate;
    }

}
