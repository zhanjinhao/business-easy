package cn.addenda.businesseasy.cache;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 抽象kv-cache的功能
 *
 * @author addenda
 * @datetime 2022/11/24 21:58
 */
public interface KVCache<K, V> {

    void set(K k, V v);

    /**
     * @param k        key
     * @param v        value
     * @param timeout  过期时间
     * @param timeunit 过期时间单位
     */
    void set(K k, V v, long timeout, TimeUnit timeunit);

    boolean containsKey(K k);

    V get(K k);

    void delete(K k);

    int size();

    int capacity();

    /**
     * get & delete
     */
    default V remove(K k) {
        V v = get(k);
        delete(k);
        return v;
    }

    default V computeIfAbsent(K key,
                              Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        V v;
        if ((v = get(key)) == null) {
            V newValue;
            if ((newValue = mappingFunction.apply(key)) != null) {
                set(key, newValue);
                return newValue;
            }
        }

        return v;
    }

}
