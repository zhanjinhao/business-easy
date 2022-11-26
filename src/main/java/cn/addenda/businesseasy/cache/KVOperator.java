package cn.addenda.businesseasy.cache;

import java.util.concurrent.TimeUnit;

/**
 * @author addenda
 * @datetime 2022/11/24 21:58
 */
public interface KVOperator<K, V> {

    void set(K k, V v);

    void set(K k, V v, long timeout, TimeUnit unit);

    V get(K k);

    void delete(K k);

    V remove(K k);

}
