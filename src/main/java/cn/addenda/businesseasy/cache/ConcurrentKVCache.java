package cn.addenda.businesseasy.cache;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * kv-cache的包装类，用于实现并发安全
 *
 * @author addenda
 * @datetime 2023/1/18 17:11
 */
public class ConcurrentKVCache<K, V> extends KVCacheWrapper<K, V> {

    private final Lock lock;

    public ConcurrentKVCache(Lock lock, KVCache<K, V> kvCacheDelegate) {
        super(kvCacheDelegate);
        this.lock = lock;
    }

    public ConcurrentKVCache(KVCache<K, V> kvCacheDelegate) {
        super(kvCacheDelegate);
        lock = new ReentrantLock();
    }

    @Override
    public void set(K k, V v) {
        lock.lock();
        try {
            getKvCacheDelegate().set(k, v);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void set(K k, V v, long timeout, TimeUnit timeunit) {
        lock.lock();
        try {
            getKvCacheDelegate().set(k, v, timeout, timeunit);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean containsKey(K k) {
        lock.lock();
        try {
            return getKvCacheDelegate().containsKey(k);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public V get(K k) {
        lock.lock();
        try {
            return getKvCacheDelegate().get(k);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void delete(K k) {
        lock.lock();
        try {
            getKvCacheDelegate().delete(k);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public V remove(K k) {
        lock.lock();
        try {
            return getKvCacheDelegate().remove(k);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        lock.lock();
        try {
            return getKvCacheDelegate().computeIfAbsent(key, mappingFunction);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int capacity() {
        return getKvCacheDelegate().capacity();
    }

}
