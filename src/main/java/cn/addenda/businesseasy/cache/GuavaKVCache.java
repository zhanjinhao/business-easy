package cn.addenda.businesseasy.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;

/**
 * @author addenda
 * @since 2023/4/19 9:29
 */
public class GuavaKVCache implements KVCache<String, String> {

    private int capacity;

    private Cache<String, String> cache;

    public GuavaKVCache(Cache<String, String> cache, int capacity) {
        this.cache = cache;
        this.capacity = capacity;
    }

    public GuavaKVCache() {
        cache = CacheBuilder.newBuilder()
            .maximumSize(1000).build();
        this.capacity = 1000;
    }

    public GuavaKVCache(int capacity) {
        cache = CacheBuilder.newBuilder()
            .maximumSize(capacity).build();
        this.capacity = capacity;
    }

    @Override
    public void set(String key, String value) {
        cache.put(key, value);
    }

    @Override
    public void set(String key, String value, long timeout, TimeUnit timeunit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(String key) {
        return cache.getIfPresent(key) != null;
    }

    @Override
    public String get(String key) {
        return cache.getIfPresent(key);
    }

    @Override
    public void delete(String key) {
        cache.invalidate(key);
    }

    @Override
    public int size() {
        return (int) cache.size();
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public String toString() {
        return "GuavaKVCache{" +
            "capacity=" + capacity +
            ", cache size=" + cache.size() +
            '}';
    }

}
