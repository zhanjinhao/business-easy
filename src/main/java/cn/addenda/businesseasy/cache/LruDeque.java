package cn.addenda.businesseasy.cache;

/**
 * LRU实现中用于存储新旧关系的双向链表
 */
public interface LruDeque<P> {

    P getFirst();

    void addLast(P p);

    void remove(P p);

    boolean contains(P p);

    int size();

}
