package cn.addenda.businesseasy.cache;

import java.util.Set;

/**
 * k有序的缓存
 *
 * @author addenda
 * @datetime 2023/1/17 19:14
 */
public interface SortedKVCache<K, V> extends KVCache<K, V> {

    K getFirst();

    K getLast();

    Set<K> keySet();

}
