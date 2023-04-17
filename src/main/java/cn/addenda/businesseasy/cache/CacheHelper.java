package cn.addenda.businesseasy.cache;

import cn.addenda.businesseasy.concurrent.SimpleNamedThreadFactory;
import cn.addenda.businesseasy.trafficlimit.RequestIntervalTrafficLimiter;
import cn.addenda.businesseasy.util.BEJsonUtils;
import cn.addenda.businesseasy.util.BESleepUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

/**
 * @author addenda
 * @datetime 2023/03/09 23:11
 */
@Slf4j
public class CacheHelper {

    public static final Long CACHE_NULL_TTL = 5 * 60 * 1000L;

    public static final String NULL_OBJECT = "_NIL";

    /**
     * ppf: performance first
     */
    public static final String PERFORMANCE_FIRST_PREFIX = "pff:";
    /**
     * rdf: realtime data first
     */
    public static final String REALTIME_DATA_FIRST_PREFIX = "rdf:";
    private static final String BUILD_CACHE_SUCCESS_MSG = "构建缓存 [{}] 成功，获取到数据 [{}]。";
    private static final String BUILD_CACHE_ERROR_MSG = "构建缓存 [{}] 失败！";

    private static final String UNEXPIRED_MSG = "获取到 [{}] 的数据 [{}] 未过期。";
    private static final String EXPIRED_MSG = "获取到 [{}] 的数据 [{}] 已过期！";
    private static final String CLEAR_MSG = "清理缓存 [{}] 成功。";

    private static final String PPF_SUBMIT_BUILD_CACHE_TASK_SUCCESS = "获取锁 [{}] 成功，提交了缓存重建任务，返回过期数据 [{}]。";
    private static final String PPF_SUBMIT_BUILD_CACHE_TASK_FAILED = "获取锁 [{}] 失败，未提交缓存重建任务，返回过期数据 [{}]。";

    private static final String RDF_TRY_LOCK_FAIL_TERMINAL = "第 [{}] 次未获取到锁 [{}]，终止获取锁";
    private static final String RDF_TRY_LOCK_FAIL_WAIT = "第 [{}] 次未获取到锁 [{}]，休眠 [{}] ms";

    private static final long WAIT_MILL_SECONDS = 50L;

    /**
     * 多个线程获取同一个锁时需要拿到同一个ReentrantLock。
     * key是锁名，value是锁。<p/>
     * 容量取决于业务线程池的大小和线程最多会拿几次锁，不然多个线程可能拿到不同的锁。
     * 如业务线程池有500个线程，每个线程最多会拿5把锁，那么容量就应该是 500*5。
     */
    private LoadingCache<String, ReentrantLock> lockCacheMap;

    /**
     * key是prefix
     */
    private final Map<String, RequestIntervalTrafficLimiter> trafficLimiterMap = new ConcurrentHashMap<>();

    private ExecutorService cacheBuildEs;

    private int rdfBusyLoop = 3;

    private final KVCache<String, String> kvCache;

    /**
     * ppf模式下过期检测间隔（ms）
     */
    private final long ppfExpirationDetectionInterval;

    public static final long DEFAULT_PPF_EXPIRATION_DETECTION_INTERVAL = 100L;
    public static final int DEFAULT_LOCK_CACHE_MAP_CAPACITY = 5000;

    public CacheHelper(KVCache<String, String> kvCache, long ppfExpirationDetectionInterval, int lockCacheMapCapacity) {
        this.kvCache = kvCache;
        this.ppfExpirationDetectionInterval = ppfExpirationDetectionInterval;
        initCacheRebuildEs();
        initLockCacheMap(lockCacheMapCapacity);
    }

    public CacheHelper(KVCache<String, String> kvCache, long ppfExpirationDetectionInterval, ExecutorService cacheBuildEs, int lockCacheMapCapacity) {
        this.kvCache = kvCache;
        this.ppfExpirationDetectionInterval = ppfExpirationDetectionInterval;
        this.cacheBuildEs = cacheBuildEs;
        initLockCacheMap(lockCacheMapCapacity);
    }

    public CacheHelper(KVCache<String, String> kvCache, long ppfExpirationDetectionInterval, ExecutorService cacheBuildEs) {
        this.kvCache = kvCache;
        this.ppfExpirationDetectionInterval = ppfExpirationDetectionInterval;
        this.cacheBuildEs = cacheBuildEs;
        initLockCacheMap(DEFAULT_LOCK_CACHE_MAP_CAPACITY);
    }

    public CacheHelper(KVCache<String, String> kvCache, long ppfExpirationDetectionInterval) {
        this.kvCache = kvCache;
        this.ppfExpirationDetectionInterval = ppfExpirationDetectionInterval;
        initCacheRebuildEs();
        initLockCacheMap(DEFAULT_LOCK_CACHE_MAP_CAPACITY);
    }

    public CacheHelper(KVCache<String, String> kvCache) {
        this.kvCache = kvCache;
        this.ppfExpirationDetectionInterval = DEFAULT_PPF_EXPIRATION_DETECTION_INTERVAL;
        initCacheRebuildEs();
        initLockCacheMap(DEFAULT_LOCK_CACHE_MAP_CAPACITY);
    }

    private void initLockCacheMap(int capacity) {
        lockCacheMap = CacheBuilder.newBuilder()
            .initialCapacity(capacity)
            .maximumSize(capacity)
            .build(new CacheLoader<String, ReentrantLock>() {
                @Override
                public ReentrantLock load(String s) throws Exception {
                    log.info("key [{}] 不存在锁或已过期，创建ReentrantLock！", s);
                    return new ReentrantLock();
                }
            });
    }

    private void initCacheRebuildEs() {
        cacheBuildEs = new ThreadPoolExecutor(
            2,
            2,
            30,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10),
            new SimpleNamedThreadFactory("CacheHelper-Rebuild"));
    }

    public <I> void acceptWithPpf(String keyPrefix, I id, Consumer<I> consumer) {
        String key = keyPrefix + PERFORMANCE_FIRST_PREFIX + id;
        consumer.accept(id);
        kvCache.remove(key);
        log.info(CLEAR_MSG, key);
    }

    public <I> void acceptWithPpfAsync(String keyPrefix, I id, Consumer<I> consumer) {
        String key = keyPrefix + PERFORMANCE_FIRST_PREFIX + id;
        consumer.accept(id);
        cacheBuildEs.execute(() -> {
            kvCache.remove(key);
            log.info(CLEAR_MSG, key);
        });
    }

    public <I, R> R applyWithPpf(String keyPrefix, I id, Function<I, R> function) {
        String key = keyPrefix + PERFORMANCE_FIRST_PREFIX + id;
        R apply = function.apply(id);
        kvCache.remove(key);
        log.info(CLEAR_MSG, key);
        return apply;
    }

    public <I, R> R applyWithPpfAsync(String keyPrefix, I id, Function<I, R> function) {
        String key = keyPrefix + PERFORMANCE_FIRST_PREFIX + id;
        R apply = function.apply(id);
        cacheBuildEs.execute(() -> {
            kvCache.remove(key);
            log.info(CLEAR_MSG, key);
        });
        return apply;
    }

    /**
     * 性能优先的缓存查询方法，基于逻辑过期实现。
     *
     * @param keyPrefix 与id一起构成完整的键
     * @param id 键值
     * @param rType 返回值类型
     * @param rtQuery 查询实时数据
     * @param ttl 过期时间
     * @param <R> 返回值类型
     * @param <I> 键值类型
     */
    public <R, I> R queryWithPpf(
        String keyPrefix, I id, Class<R> rType, Function<I, R> rtQuery, Long ttl) {
        TypeReference<R> typeReference = new TypeReference<R>() {
            @Override
            public Type getType() {
                return rType;
            }
        };
        return queryWithPpf(keyPrefix, id, typeReference, rtQuery, ttl);
    }

    /**
     * 性能优先的缓存查询方法，基于逻辑过期实现。
     *
     * @param keyPrefix 与id一起构成完整的键
     * @param id 键值
     * @param rType 返回值类型
     * @param rtQuery 查询实时数据
     * @param ttl 过期时间
     * @param <R> 返回值类型
     * @param <I> 键值类型
     */
    public <R, I> R queryWithPpf(
        String keyPrefix, I id, TypeReference<R> rType, Function<I, R> rtQuery, Long ttl) {
        String key = keyPrefix + PERFORMANCE_FIRST_PREFIX + id;
        // 1 查询缓存
        String cachedJson = kvCache.get(key);
        // 2.1 缓存不存在则基于互斥锁构建缓存
        if (cachedJson == null) {
            // 查询数据库
            R r = queryWithRdf(keyPrefix, id, rType, rtQuery, ttl, false);
            // 存在缓存里
            setCacheData(key, r, ttl);
            return r;
        }
        // 2.2 缓存存在则进入逻辑过期的判断
        else {
            // 3.1 命中，需要先把json反序列化为对象
            CacheData<R> cacheData = (CacheData<R>) BEJsonUtils.stringToObject(cachedJson, createRdTypeReference(rType));
            LocalDateTime expireTime = cacheData.getExpireTime();
            R data = cacheData.getData();
            // 4.1 判断是否过期，未过期，直接返回
            if (expireTime.isAfter(LocalDateTime.now())) {
                log.debug(UNEXPIRED_MSG, key, data);
            }
            // 4.2 判断是否过期，已过期，需要缓存重建
            else {
                // 5.1 获取互斥锁，成功，开启独立线程，进行缓存重建
                Lock lock;
                try {
                    lock = lockCacheMap.get(getLockKey(key));
                } catch (ExecutionException e) {
                    throw new CacheException("获取互斥锁异常：" + getLockKey(key) + "。", e, CacheException.ERROR);
                }
                AtomicBoolean newDataReady = new AtomicBoolean(false);
                if (lock.tryLock()) {
                    try {
                        cacheBuildEs.submit(() -> {
                            try {
                                // 查询数据库
                                R r = rtQuery.apply(id);
                                // 存在缓存里
                                setCacheData(key, r, ttl);
                                newDataReady.set(true);
                            } catch (Exception e) {
                                log.error(BUILD_CACHE_ERROR_MSG, key, e);
                            }
                        });
                        // 提交完缓存构建任务后休息一段时间，防止其他线程提交缓存构建任务
                        BESleepUtils.sleep(TimeUnit.MILLISECONDS, WAIT_MILL_SECONDS);
                        if (newDataReady.get()) {
                            return queryWithPpf(keyPrefix, id, rType, rtQuery, ttl);
                        } else {
                            log.info(PPF_SUBMIT_BUILD_CACHE_TASK_SUCCESS, getLockKey(key), data);
                        }
                    } finally {
                        lock.unlock();
                    }
                }
                // 5.2 获取互斥锁，未成功不进行缓存重建
                else {
                    log.info(PPF_SUBMIT_BUILD_CACHE_TASK_FAILED, getLockKey(key), data);
                }
                // 如果过期了，输出告警信息。
                // 使用限流器防止高并发下大量打印日志。
                RequestIntervalTrafficLimiter trafficLimiter = trafficLimiterMap.computeIfAbsent(
                    keyPrefix + PERFORMANCE_FIRST_PREFIX, s -> new RequestIntervalTrafficLimiter(ppfExpirationDetectionInterval));
                if (trafficLimiter.acquire()) {
                    log.error(EXPIRED_MSG, key, data);
                }
            }
            return data;
        }
    }

    private <R> void setCacheData(String key, R r, long ttl) {
        // 设置逻辑过期
        CacheData<R> newCacheData = new CacheData<>(r);
        if (r == null) {
            newCacheData.setExpireTime(LocalDateTime.now().plus(Math.min(ttl, CACHE_NULL_TTL), ChronoUnit.MILLIS));
        } else {
            newCacheData.setExpireTime(LocalDateTime.now().plus(ttl, ChronoUnit.MILLIS));
        }
        // 写缓存
        kvCache.set(key, BEJsonUtils.objectToString(newCacheData));
        log.info(BUILD_CACHE_SUCCESS_MSG, key, r);
    }

    public <I> void acceptWithRdf(String keyPrefix, I id, Consumer<I> consumer) {
        String key = keyPrefix + REALTIME_DATA_FIRST_PREFIX + id;
        consumer.accept(id);
        kvCache.remove(key);
        log.info(CLEAR_MSG, key);
    }

    public <I> void acceptWithRdfAsync(String keyPrefix, I id, Consumer<I> consumer) {
        String key = keyPrefix + REALTIME_DATA_FIRST_PREFIX + id;
        consumer.accept(id);
        cacheBuildEs.execute(() -> {
            kvCache.remove(key);
            log.info(CLEAR_MSG, key);
        });
    }

    public <I, R> R applyWithRdf(String keyPrefix, I id, Function<I, R> function) {
        String key = keyPrefix + REALTIME_DATA_FIRST_PREFIX + id;
        R apply = function.apply(id);
        kvCache.remove(key);
        log.info(CLEAR_MSG, key);
        return apply;
    }

    public <I, R> R applyWithRdfAsync(String keyPrefix, I id, Function<I, R> function) {
        String key = keyPrefix + REALTIME_DATA_FIRST_PREFIX + id;
        R apply = function.apply(id);
        cacheBuildEs.execute(() -> {
            kvCache.remove(key);
            log.info(CLEAR_MSG, key);
        });
        return apply;
    }

    /**
     * 实时数据优先的缓存查询方法，基于互斥锁实现。
     *
     * @param keyPrefix 与id一起构成完整的键
     * @param id 键值
     * @param rType 返回值类型
     * @param rtQuery 查询实时数据
     * @param ttl 过期时间
     * @param <R> 返回值类型
     * @param <I> 键值类型
     */
    public <R, I> R queryWithRdf(
        String keyPrefix, I id, TypeReference<R> rType, Function<I, R> rtQuery, Long ttl) {
        return queryWithRdf(keyPrefix, id, rType, rtQuery, ttl, true);
    }

    /**
     * 实时数据优先的缓存查询方法，基于互持锁实现。
     *
     * @param keyPrefix 与id一起构成完整的键
     * @param id 键值
     * @param rType 返回值类型
     * @param rtQuery 查询实时数据
     * @param ttl 过期时间
     * @param <R> 返回值类型
     * @param <I> 键值类型
     */
    public <R, I> R queryWithRdf(
        String keyPrefix, I id, Class<R> rType, Function<I, R> rtQuery, Long ttl) {
        return queryWithRdf(keyPrefix, id, rType, rtQuery, ttl, true);
    }

    /**
     * 实时数据优先的缓存查询方法，基于互持锁实现。
     *
     * @param keyPrefix 与id一起构成完整的键
     * @param id 键值
     * @param rType 返回值类型
     * @param rtQuery 查询实时数据
     * @param ttl 过期时间
     * @param cache 是否将实时查询的数据缓存
     * @param <R> 返回值类型
     * @param <I> 键值类型
     */
    private <R, I> R queryWithRdf(
        String keyPrefix, I id, TypeReference<R> rType, Function<I, R> rtQuery, Long ttl, boolean cache) {
        return doQueryWithRdf(keyPrefix, id, rType, rtQuery, ttl, 0, cache);
    }

    /**
     * 实时数据优先的缓存查询方法，基于互持锁实现。
     *
     * @param keyPrefix 与id一起构成完整的键
     * @param id 键值
     * @param rType 返回值类型
     * @param rtQuery 查询实时数据
     * @param ttl 过期时间
     * @param cache 是否将实时查询的数据缓存
     * @param <R> 返回值类型
     * @param <I> 键值类型
     */
    private <R, I> R queryWithRdf(
        String keyPrefix, I id, Class<R> rType, Function<I, R> rtQuery, Long ttl, boolean cache) {
        TypeReference<R> typeReference = new TypeReference<R>() {
            @Override
            public Type getType() {
                return rType;
            }
        };
        return doQueryWithRdf(keyPrefix, id, typeReference, rtQuery, ttl, 0, cache);
    }

    /**
     * 实时数据优先的缓存查询方法，基于互持锁实现。
     *
     * @param keyPrefix 与id一起构成完整的键
     * @param id 键值
     * @param rType 返回值类型
     * @param rtQuery 查询实时数据
     * @param ttl 过期时间
     * @param itr 第几次尝试
     * @param cache 是否将实时查询的数据缓存
     * @param <R> 返回值类型
     * @param <I> 键值类型
     */
    private <R, I> R doQueryWithRdf(
        String keyPrefix, I id, TypeReference<R> rType, Function<I, R> rtQuery, Long ttl, int itr, boolean cache) {
        String key = keyPrefix + REALTIME_DATA_FIRST_PREFIX + id;
        // 1.查询缓存
        String resultJson = kvCache.get(key);
        // 2.如果返回的是占位的空值，返回null
        if (NULL_OBJECT.equals(resultJson)) {
            log.debug("获取到 [{}] 的数据为空占位。", key);
            return null;
        }
        // 3.1如果字符串不为空，返回对象
        if (resultJson != null) {
            log.debug(UNEXPIRED_MSG, key, resultJson);
            return BEJsonUtils.stringToObject(resultJson, rType);
        }
        // 3.2如果字符串为空，进行缓存构建
        else {
            // 4.1获取互斥锁，获取到进行缓存构建
            // 5.1 获取互斥锁，成功，开启独立线程，进行缓存重建
            Lock lock;
            try {
                lock = lockCacheMap.get(getLockKey(key));
            } catch (ExecutionException e) {
                throw new CacheException("获取互斥锁异常：" + getLockKey(key) + "。", e, CacheException.ERROR);
            }
            if (lock.tryLock()) {
                try {
                    R r = rtQuery.apply(id);
                    if (cache) {
                        if (r == null) {
                            kvCache.set(key, NULL_OBJECT, Math.min(CACHE_NULL_TTL, ttl), TimeUnit.MILLISECONDS);
                        } else {
                            kvCache.set(key, BEJsonUtils.objectToString(r), ttl, TimeUnit.MILLISECONDS);
                        }
                        log.info(BUILD_CACHE_SUCCESS_MSG, key, r);
                    }
                    return r;
                } catch (Exception e) {
                    log.error(BUILD_CACHE_ERROR_MSG, key, e);
                    throw new CacheException("构建缓存 [" + key + "] 失败！", e, CacheException.ERROR);
                } finally {
                    lock.unlock();
                }
            }
            // 4.2获取互斥锁，获取不到就休眠直至抛出异常
            else {
                itr++;
                if (itr == rdfBusyLoop) {
                    log.error(RDF_TRY_LOCK_FAIL_TERMINAL, itr, getLockKey(key));
                    throw new CacheException("系统繁忙，请稍后再试！", CacheException.BUSY);
                } else {
                    log.info(RDF_TRY_LOCK_FAIL_WAIT, itr, getLockKey(key), WAIT_MILL_SECONDS);
                    BESleepUtils.sleep(TimeUnit.MILLISECONDS, WAIT_MILL_SECONDS);
                    return doQueryWithRdf(keyPrefix, id, rType, rtQuery, ttl, itr, cache);
                }
            }
        }
    }

    public int getRdfBusyLoop() {
        return rdfBusyLoop;
    }

    public void setRdfBusyLoop(int rdfBusyLoop) {
        this.rdfBusyLoop = rdfBusyLoop;
    }

    private String getLockKey(String key) {
        return key + ":lock";
    }

    private <R> TypeReference<?> createRdTypeReference(TypeReference<R> typeReference) {
        TypeReference<Object> reference = new TypeReference<Object>() {
        };
        try {
            ParameterizedType a = ParameterizedTypeImpl.make(CacheData.class, new Type[]{typeReference.getType()}, null);
            Field typeField = TypeReference.class.getDeclaredField("_type");
            typeField.setAccessible(true);
            typeField.set(reference, a);
            return reference;
        } catch (IllegalAccessException | NoSuchFieldException e) {
            log.error("无法设置 TypeReference 的 _type 属性！", e);
            throw new CacheException("无法设置 TypeReference 的 _type 属性！", CacheException.ERROR);
        }
    }

}
