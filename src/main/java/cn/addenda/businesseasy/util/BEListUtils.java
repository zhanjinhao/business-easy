package cn.addenda.businesseasy.util;

import cn.addenda.businesseasy.asynctask.TernaryResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * @author addenda
 * @since 2022/2/7 12:38
 */
@Slf4j
public class BEListUtils {

    private static final int BATCH_SIZE = 100;

    private BEListUtils() {
        throw new BEUtilsException("工具类不可实例化！");
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

    public static <T, R extends Comparable<? super R>> List<T> deDuplicate(List<T> list, Function<T, R> function) {
        return deDuplicate(list, Comparator.comparing(function), ArrayList::new);
    }

    public static <T> List<T> deDuplicate(List<T> list, Comparator<T> comparator) {
        return deDuplicate(list, comparator, ArrayList::new);
    }

    public static <T> List<T> deDuplicate(List<T> list, Comparator<T> comparator, Function<TreeSet<T>, List<T>> finisher) {
        if (list == null) {
            return null;
        }
        log.debug("去重前 size: {}，集合: {}", list.size(), BEJsonUtils.objectToString(list));
        List<T> collect = list.stream().collect(Collectors.collectingAndThen(
            Collectors.toCollection(() -> new TreeSet<>(comparator)), finisher));
        log.debug("去重后 size: {}，集合: {}", collect.size(), BEJsonUtils.objectToString(collect));
        return collect;
    }

    public static <T> void acceptInBatches(List<T> paramList, Consumer<List<T>> consumer) {
        applyInBatches(paramList, new VoidFunction(consumer), BATCH_SIZE, null);
    }

    public static <R, T> List<R> applyInBatches(List<T> paramList, Function<List<T>, List<R>> function) {
        return applyInBatches(paramList, function, BATCH_SIZE, null);
    }

    public static <T> void acceptInBatches(List<T> paramList, Consumer<List<T>> consumer, int batchSize) {
        applyInBatches(paramList, new VoidFunction(consumer), batchSize, null);
    }

    public static <R, T> List<R> applyInBatches(List<T> paramList, Function<List<T>, List<R>> function, int batchSize) {
        return applyInBatches(paramList, function, batchSize, null);
    }

    public static <T> void acceptInBatches(List<T> paramList, Consumer<List<T>> consumer, String name) {
        applyInBatches(paramList, new VoidFunction(consumer), BATCH_SIZE, name);
    }

    public static <R, T> List<R> applyInBatches(List<T> paramList, Function<List<T>, List<R>> function, String name) {
        return applyInBatches(paramList, function, BATCH_SIZE, name);
    }

    public static <T> void acceptInBatches(List<T> paramList, Consumer<List<T>> consumer, int batchSize, String name) {
        applyInBatches(paramList, new VoidFunction(consumer), batchSize, name);
    }

    /**
     * @param paramList 参数集合
     * @param function 参数集合映射到结果集合的函数
     * @param batchSize 一次处理多少参数
     * @param name 给当前操作起个名字，方便排查问题
     * @return 结果
     */
    public static <R, T> List<R> applyInBatches(List<T> paramList, Function<List<T>, List<R>> function, int batchSize, String name) {
        if (paramList == null || paramList.isEmpty()) {
            return new ArrayList<>();
        }
        if (batchSize <= 0) {
            throw new BEUtilsException("applyInBatches 的 batchSize 必须大于1，当前是: " + batchSize + ". ");
        }
        long start = System.currentTimeMillis();
        List<R> result = new ArrayList<>();
        List<List<T>> paramSegList = splitList(paramList, batchSize);
        for (int i = 0; i < paramSegList.size(); i++) {
            List<T> paramSeg = paramSegList.get(i);
            log.debug("Seg-{}-param: {}", i, BEJsonUtils.objectToString(paramSeg));
            List<R> resultSeg = function.apply(paramSeg);
            if (!(function instanceof VoidFunction)) {
                log.debug("Seg-{}-result: {}", i, BEJsonUtils.objectToString(resultSeg));
            }
            if (resultSeg != null) {
                result.addAll(resultSeg);
            }
        }
        if (name != null) {
            log.info("applyInBatches [{}] operation execute [{}] ms. ", name, System.currentTimeMillis() - start);
        } else {
            log.info("applyInBatches operation execute [{}] ms. ", System.currentTimeMillis() - start);
        }
        return result;
    }


    public static <T1, T2> void acceptInBatches(
        List<T1> param1List, List<T2> param2List, BiConsumer<List<T1>, List<T2>> consumer) {
        applyInBatches(param1List, param2List, new VoidBiFunction<>(consumer), BATCH_SIZE, null);
    }


    public static <R, T1, T2> List<R> applyInBatches(
        List<T1> param1List, List<T2> param2List, BiFunction<List<T1>, List<T2>, List<R>> function) {
        return applyInBatches(param1List, param2List, function, BATCH_SIZE, null);
    }

    public static <T1, T2> void acceptInBatches(
        List<T1> param1List, List<T2> param2List, BiConsumer<List<T1>, List<T2>> consumer, int batchSize) {
        applyInBatches(param1List, param2List, new VoidBiFunction<>(consumer), batchSize, null);
    }

    public static <R, T1, T2> List<R> applyInBatches(
        List<T1> param1List, List<T2> param2List, BiFunction<List<T1>, List<T2>, List<R>> function, int batchSize) {
        return applyInBatches(param1List, param2List, function, batchSize, null);
    }

    public static <T1, T2> void acceptInBatches(
        List<T1> param1List, List<T2> param2List, BiConsumer<List<T1>, List<T2>> consumer, String name) {
        applyInBatches(param1List, param2List, new VoidBiFunction<>(consumer), BATCH_SIZE, name);
    }

    public static <R, T1, T2> List<R> applyInBatches(
        List<T1> param1List, List<T2> param2List, BiFunction<List<T1>, List<T2>, List<R>> function, String name) {
        return applyInBatches(param1List, param2List, function, BATCH_SIZE, name);
    }

    public static <T1, T2> void acceptInBatches(
        List<T1> param1List, List<T2> param2List, BiConsumer<List<T1>, List<T2>> consumer, int batchSize, String name) {
        applyInBatches(param1List, param2List, new VoidBiFunction<>(consumer), batchSize, name);
    }

    public static <R, T1, T2> List<R> applyInBatches(
        List<T1> param1List, List<T2> param2List, BiFunction<List<T1>, List<T2>, List<R>> function, int batchSize, String name) {
        if (param1List == null || param1List.isEmpty() || param2List == null || param2List.isEmpty()) {
            return new ArrayList<>();
        }
        if (batchSize <= 0) {
            throw new BEUtilsException("applyInBatches batchSize 必须大于1，当前是: " + batchSize + ". ");
        }
        long start = System.currentTimeMillis();
        List<R> result = new ArrayList<>();
        List<List<T1>> param1SegList = splitList(param1List, batchSize);
        for (int i = 0; i < param1SegList.size(); i++) {
            List<T1> param1Seg = param1SegList.get(i);
            List<List<T2>> param2SegList = splitList(param2List, batchSize);
            for (int j = 0; j < param2SegList.size(); j++) {
                List<T2> param2Seg = param2SegList.get(j);
                log.debug("Seg-{}-{}-param: {}, {}", i, j, BEJsonUtils.objectToString(param1Seg), BEJsonUtils.objectToString(param2Seg));
                List<R> resultSeg = function.apply(param1Seg, param2Seg);
                if (!(function instanceof VoidBiFunction)) {
                    log.debug("Seg-{}-{}-result: {}", i, j, BEJsonUtils.objectToString(resultSeg));
                }
                if (resultSeg != null) {
                    result.addAll(resultSeg);
                }
            }
        }
        if (name != null) {
            log.info("applyInBatches {} operation execute [{}] ms. ", name, System.currentTimeMillis() - start);
        } else {
            log.info("applyInBatches operation execute [{}] ms. ", System.currentTimeMillis() - start);
        }
        return result;
    }


    private static class VoidFunction<T, R> implements Function<T, R> {

        private final Consumer<T> consumer;

        public VoidFunction(Consumer<T> consumer) {
            this.consumer = consumer;
        }

        @Override
        public R apply(T o) {
            consumer.accept(o);
            return null;
        }
    }

    private static class VoidBiFunction<T, U, R> implements BiFunction<T, U, R> {

        private final BiConsumer<T, U> consumer;

        public VoidBiFunction(BiConsumer<T, U> consumer) {
            this.consumer = consumer;
        }

        @Override
        public R apply(T t, U u) {
            consumer.accept(t, u);
            return null;
        }
    }

}
