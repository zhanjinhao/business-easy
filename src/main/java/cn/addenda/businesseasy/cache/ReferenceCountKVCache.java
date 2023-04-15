package cn.addenda.businesseasy.cache;

public interface ReferenceCountKVCache<K, V> extends KVCache<K, V> {

    void release(K k);

}
