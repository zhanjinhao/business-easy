package cn.addenda.businesseasy.util;

import cn.addenda.businesseasy.asynctask.TernaryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @Author ISJINHAO
 * @Date 2022/2/7 12:38
 */
@Slf4j
public class BEListUtil {

    private static final int BATCH_SIZE = 100;

    private BEListUtil() {
        throw new BEUtilException("工具类不可实例化！");
    }

    /**
     * 集合做拆分
     */
    public static <T> List<List<T>> splitList(List<T> list, int quantity) {
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        quantity = quantity <= 0 ? list.size() : quantity;
        List<List<T>> splitList = new ArrayList<>();
        int count = 0;
        while (count < list.size()) {
            splitList.add(new ArrayList<>(list.subList(count, Math.min((count + quantity), list.size()))));
            count += quantity;
        }
        return splitList;
    }


    public static <T> TernaryResult<List<T>, List<T>, List<T>> separate(List<T> a, List<T> b) {
        a = new ArrayList<>(a);
        b = new ArrayList<>(b);
        List<T> inAButNotInB = new ArrayList<>();
        List<T> inAAndB = new ArrayList<>();

        for (T t : a) {
            Iterator<T> iterator = b.iterator();
            boolean fg = false;
            while (iterator.hasNext()) {
                T next = iterator.next();
                if (t.equals(next)) {
                    inAAndB.add(t);
                    iterator.remove();
                    fg = true;
                    break;
                }
            }
            if (!fg) {
                inAButNotInB.add(t);
            }
        }

        List<T> notInAButInB = new ArrayList<>(b);

        return new TernaryResult<>(inAButNotInB, inAAndB, notInAButInB);
    }

    public static <T> List<T> merge(List<T> a, List<T> b) {
        if (a == null && b == null) {
            return new ArrayList<>();
        }
        if (a == null) {
            return new ArrayList<>(b);
        }
        if (b == null) {
            return new ArrayList<>(a);
        }
        List<T> result = new ArrayList<>();
        result.addAll(a);
        result.addAll(b);
        return result;
    }

    public static <R, T> List<R> inList(List<T> paramList, Function<List<T>, List<R>> function) {
        return inList(paramList, function, BATCH_SIZE, null);
    }

    public static <R, T> List<R> inList(List<T> paramList, Function<List<T>, List<R>> function, int batchSize) {
        return inList(paramList, function, batchSize, null);
    }

    public static <R, T> List<R> inList(List<T> paramList, Function<List<T>, List<R>> function, String name) {
        return inList(paramList, function, BATCH_SIZE, name);
    }

    public static <R, T> List<R> inList(List<T> paramList, Function<List<T>, List<R>> function, int batchSize, String name) {
        if (paramList == null || paramList.isEmpty()) {
            return new ArrayList<>();
        }
        if (batchSize <= 0) {
            throw new BEUtilException("inList batchSize 必须大于1，当前是: " + batchSize + ". ");
        }
        long start = System.currentTimeMillis();
        List<R> result = new ArrayList<>();
        List<List<T>> paramListList = splitList(paramList, batchSize);
        for (List<T> paramSeg : paramListList) {
            List<R> apply = function.apply(paramSeg);
            if (apply != null) {
                result.addAll(apply);
            }
        }
        if (name != null) {
            log.info("inList {} operation execute [{}] ms. ", name, System.currentTimeMillis() - start);
        } else {
            log.info("inList operation execute [{}] ms. ", System.currentTimeMillis() - start);
        }
        return result;
    }

    public static <R, T1, T2> List<R> inList(List<T1> param1List, List<T2> param2List, BiFunction<List<T1>, List<T2>, List<R>> function) {
        return inList(param1List, param2List, function, BATCH_SIZE, null);
    }

    public static <R, T1, T2> List<R> inList(List<T1> param1List, List<T2> param2List, BiFunction<List<T1>, List<T2>, List<R>> function, int batchSize) {
        return inList(param1List, param2List, function, batchSize, null);
    }

    public static <R, T1, T2> List<R> inList(List<T1> param1List, List<T2> param2List, BiFunction<List<T1>, List<T2>, List<R>> function, String name) {
        return inList(param1List, param2List, function, BATCH_SIZE, name);
    }

    public static <R, T1, T2> List<R> inList(List<T1> param1List, List<T2> param2List,
                                             BiFunction<List<T1>, List<T2>, List<R>> function, int batchSize, String name) {
        if (param1List == null || param1List.isEmpty() || param2List == null || param2List.isEmpty()) {
            return new ArrayList<>();
        }
        if (batchSize <= 0) {
            throw new BEUtilException("inList batchSize 必须大于1，当前是: " + batchSize + ". ");
        }
        long start = System.currentTimeMillis();
        List<R> result = new ArrayList<>();
        List<List<T1>> param1ListList = splitList(param1List, batchSize);
        for (List<T1> param1Seg : param1ListList) {
            List<List<T2>> param2ListList = splitList(param2List, batchSize);
            for (List<T2> param2Seg : param2ListList) {
                List<R> apply = function.apply(param1Seg, param2Seg);
                if (apply != null) {
                    result.addAll(apply);
                }
            }
        }
        if (name != null) {
            log.info("inList {} operation execute [{}] ms. ", name, System.currentTimeMillis() - start);
        } else {
            log.info("inList operation execute [{}] ms. ", System.currentTimeMillis() - start);
        }
        return result;
    }

}
