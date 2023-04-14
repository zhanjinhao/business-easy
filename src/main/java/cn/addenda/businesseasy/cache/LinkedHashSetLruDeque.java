package cn.addenda.businesseasy.cache;

import java.util.LinkedHashSet;

/**
 * @author addenda
 * @datetime 2023/1/17 13:54
 */
public class LinkedHashSetLruDeque<P> implements LruDeque<P> {

    private final LinkedHashSet<P> linkedHashSet = new LinkedHashSet<>();

    @Override
    public P getFirst() {
        if (linkedHashSet.isEmpty()) {
            return null;
        }
        return linkedHashSet.iterator().next();
    }

    @Override
    public void addLast(P p) {
        linkedHashSet.add(p);
    }

    @Override
    public void remove(P p) {
        linkedHashSet.remove(p);
    }

    @Override
    public boolean contains(P p) {
        return linkedHashSet.contains(p);
    }

    @Override
    public int size() {
        return linkedHashSet.size();
    }

}
