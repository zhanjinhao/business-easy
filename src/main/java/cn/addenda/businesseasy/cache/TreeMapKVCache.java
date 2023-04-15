package cn.addenda.businesseasy.cache;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author addenda
 * @datetime 2023/1/18 11:17
 */
public class TreeMapKVCache<K, V> extends SortedMapKVCache<K, V> {

    public TreeMapKVCache() {
        super(new TreeMap<>());
    }

    public TreeMapKVCache(SortedMap<K, V> sortedMap) {
        super(sortedMap);
    }

}
