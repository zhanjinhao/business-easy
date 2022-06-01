package cn.addenda.businesseasy.asynctask;

import cn.addenda.businesseasy.util.BEArrayUtil;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 01395265
 * @date 2022/5/19
 */
@Slf4j
public class AsyncTaskExecutorService {

    private AsyncTaskExecutorService() {

    }

    private static final ExecutorService executorService =
        new ThreadPoolExecutor(8, 8, 1, TimeUnit.MINUTES,
            new LinkedBlockingQueue<>(), new SimpleNamedThreadFactory("AsyncTaskExecutorService"));

    /**
     * 提交一个异步任务
     */
    public static <T> CompletableFuture<FutureResult<T>> supplyAsync(Supplier<FutureResult<T>> supplier) {
        return CompletableFuture.supplyAsync(supplier, executorService)
            .exceptionally(e -> new FutureResult<>(BEArrayUtil.asArrayList(e)));
    }

    /**
     * 提交一个异步任务
     */
    public static <T> CompletableFuture<FutureResult<T>> supplyAsync(Supplier<FutureResult<T>> supplier, String name) {
        return CompletableFuture.supplyAsync(costWrap(supplier, name), executorService)
            .exceptionally(e -> new FutureResult<>(BEArrayUtil.asArrayList(e)));
    }

    /**
     * future执行完成之后，回调function
     */
    public static <T, U> CompletableFuture<FutureResult<U>> thenApplyAsync(CompletableFuture<FutureResult<T>> future,
        Function<FutureResult<T>, FutureResult<U>> function) {
        return future.thenApplyAsync(function, executorService)
            .exceptionally(e -> new FutureResult<>(BEArrayUtil.asArrayList(e)));
    }

    /**
     * future执行完成之后，回调function
     */
    public static <T, U> CompletableFuture<FutureResult<U>> thenApplyAsync(CompletableFuture<FutureResult<T>> future,
        Function<FutureResult<T>, FutureResult<U>> function, String name) {
        return future.thenApplyAsync(costWrap(function, name), executorService)
            .exceptionally(e -> new FutureResult<>(BEArrayUtil.asArrayList(e)));
    }

    /**
     * future和otherFuture都执行完成之后，回调function
     */
    public static <T, P, U> CompletableFuture<FutureResult<U>> thenCombineAsync(CompletableFuture<FutureResult<T>> future,
        CompletableFuture<FutureResult<P>> otherFuture, BiFunction<FutureResult<T>, FutureResult<P>, FutureResult<U>> function) {
        return future.thenCombineAsync(otherFuture, function, executorService)
            .exceptionally(e -> new FutureResult<>(BEArrayUtil.asArrayList(e)));
    }

    /**
     * future和otherFuture都执行完成之后，回调function
     */
    public static <T, P, U> CompletableFuture<FutureResult<U>> thenCombineAsync(CompletableFuture<FutureResult<T>> future,
        CompletableFuture<FutureResult<P>> otherFuture, BiFunction<FutureResult<T>, FutureResult<P>, FutureResult<U>> function, String name) {
        return future.thenCombineAsync(otherFuture, costWrap(function, name), executorService)
            .exceptionally(e -> new FutureResult<>(BEArrayUtil.asArrayList(e)));
    }

    /**
     * 当前线程阻塞至所有的future执行完成
     */
    public static void allOfComplete(CompletableFuture<?>... futures) {
        CompletableFuture.allOf(futures).join();
    }

    /**
     * 从完成的 future 中获取结果。
     */
    public static <T> T retrieveResultNow(CompletableFuture<FutureResult<T>> future) {
        return retrieveFutureResultNow(future).getResultWithThrowFirstThrowable();
    }

    /**
     * 从完成的 future 中获取 FutureResult。
     */
    public static <T> FutureResult<T> retrieveFutureResultNow(CompletableFuture<FutureResult<T>> future) {
        if (!future.isDone()) {
            throw new AsyncTaskException("不能从未完成的future里获取数据！");
        }
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("从 CompletableFuture<FutureResult<T>> 获取结果失败！", e);
            Thread.currentThread().interrupt();
            throw new AsyncTaskException(e);
        }
    }

    private static <T> Supplier<T> costWrap(Supplier<T> supplier, String name) {
        return () -> {
            long start = System.currentTimeMillis();
            T t = supplier.get();
            log.info("{} execute time : {} .", name, (System.currentTimeMillis() - start));
            return t;
        };
    }

    private static <T, R> Function<T, R> costWrap(Function<T, R> function, String name) {
        return t -> {
            long start = System.currentTimeMillis();
            R apply = function.apply(t);
            log.info("{} execute time : {} .", name, (System.currentTimeMillis() - start));
            return apply;
        };
    }

    private static <T, P, R> BiFunction<T, P, R> costWrap(BiFunction<T, P, R> function, String name) {
        return (t, p) -> {
            long start = System.currentTimeMillis();
            R apply = function.apply(t, p);
            log.info("{} execute time : {} .", name, (System.currentTimeMillis() - start));
            return apply;
        };
    }

    /**
     * copy from apache-dubbo, simplify its implementation.
     * InternalThreadFactory.
     */
    public static class SimpleNamedThreadFactory implements ThreadFactory {

        private final String mPrefix;

        public SimpleNamedThreadFactory(String mPrefix) {
            this.mPrefix = mPrefix;
        }

        private final AtomicInteger mThreadNum = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            String name = mPrefix + "-" + mThreadNum.getAndIncrement();
            return new Thread(runnable, name);
        }

    }

}
