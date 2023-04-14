package cn.addenda.businesseasy.cache;

import java.util.concurrent.TimeUnit;

/**
 * LRU缓存：基础的LRU缓存，基于双向链表和KV存储实现
 *
 * @author addenda
 * @datetime 2022/12/28 20:08
 */
public class LruKVCache<K, V> extends EliminableKVCache<K, V> {

    /**
     * 记录新旧关系的双向队列：尾节点最新头节点最旧
     */
    private final LruDeque<K> lruDeque;

    public LruKVCache(int capacity, KVCache<K, V> kvCacheDelegate) {
        super(capacity, kvCacheDelegate);
        lruDeque = new LinkedHashSetLruDeque<>();
    }

    public LruKVCache(LruDeque<K> lruDeque, int capacity, KVCache<K, V> kvCacheDelegate) {
        super(capacity, kvCacheDelegate);
        this.lruDeque = lruDeque;
    }

    /**
     * 如果k已存在，将k移至尾节点，更新cache
     * <p>
     * k不存在且缓存已满，移除头节点
     * <p>
     * k不存在时，将k插入尾节点，存cache
     */

    @Override
    protected void setWhenContainsKey(K k, V v) {
        delete(k);

        lruDeque.addLast(k);
        getKvCacheDelegate().set(k, v);
    }

    @Override
    protected void setWhenNonContainsKey(K k, V v) {
        if (capacity() == lruDeque.size()) {
            delete(lruDeque.getFirst());
        }

        lruDeque.addLast(k);
        getKvCacheDelegate().set(k, v);
    }


    /**
     * 如果k已存在，将k移至尾节点，更新cache
     * <p>
     * k不存在且缓存已满，移除头节点
     * <p>
     * k不存在时，将k插入尾节点，存cache
     */

    @Override
    protected void setWhenContainsKey(K k, V v, long timeout, TimeUnit timeunit) {
        delete(k);

        lruDeque.addLast(k);
        getKvCacheDelegate().set(k, v, timeout, timeunit);
    }

    @Override
    protected void setWhenNonContainsKey(K k, V v, long timeout, TimeUnit timeunit) {
        if (capacity() == lruDeque.size()) {
            delete(lruDeque.getFirst());
        }

        lruDeque.addLast(k);
        getKvCacheDelegate().set(k, v);
    }

    @Override
    protected void clearKeyUsage(K k) {
        lruDeque.remove(k);
    }

    /**
     * 如果cache不存在k，返回null;
     * <p>
     * 如果cache存在k，返回v，同时将k移至尾节点。
     */

    @Override
    protected V getWhenNonContainsKey(K k) {
        return null;
    }

    @Override
    protected V getWhenContainsKey(K k) {
        lruDeque.remove(k);
        lruDeque.addLast(k);
        return getKvCacheDelegate().get(k);
    }

    /**
     * 同时删除cache里的数据和lruDeque里的数据
     */
    @Override
    public void delete(K k) {
        clearKeyUsage(k);
        getKvCacheDelegate().delete(k);
    }

    protected LruDeque<K> getLruDeque() {
        return lruDeque;
    }

    protected void onlySet(K k, V v) {
        getKvCacheDelegate().set(k, v);
    }

    protected V onlyGet(K k) {
        return getKvCacheDelegate().get(k);
    }

}
