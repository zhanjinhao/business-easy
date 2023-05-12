package cn.addenda.businesseasy.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author addenda
 * @since 2022/2/7 12:37
 */
public class BEArrayUtils {

    private BEArrayUtils() {
        throw new BEUtilsException("工具类不可实例化！");
    }

    public static <T> List<T> asArrayList(T... objs) {
        List<T> list = new ArrayList<>();
        Collections.addAll(list, objs);
        return list;
    }

    public static <T> List<T> asLinkedList(T... objs) {
        List<T> list = new LinkedList<>();
        Collections.addAll(list, objs);
        return list;
    }

    public static <T> Set<T> asHashSet(T... objs) {
        Set<T> set = new HashSet<>();
        Collections.addAll(set, objs);
        return set;
    }

    public static <T> Set<T> asTreeSet(Comparator<T> comparator, T... objs) {
        Set<T> set = new TreeSet<>(comparator);
        Collections.addAll(set, objs);
        return set;
    }

    public static <T extends Comparable<? super T>> Set<T> asTreeSet(T... objs) {
        Set<T> set = new TreeSet<>();
        Collections.addAll(set, objs);
        return set;
    }

}
