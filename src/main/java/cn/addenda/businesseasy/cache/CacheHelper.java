package cn.addenda.businesseasy.cache;

import cn.addenda.businesseasy.concurrent.SimpleNamedThreadFactory;
import cn.addenda.businesseasy.json.LocalDateTimeStrDeSerializer;
import cn.addenda.businesseasy.json.LocalDateTimeStrSerializer;
import cn.addenda.businesseasy.lock.LockService;
import cn.addenda.businesseasy.trafficlimit.RequestIntervalTrafficLimiter;
import cn.addenda.businesseasy.trafficlimit.TrafficLimiter;
import cn.addenda.businesseasy.util.BEJsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class CacheHelper {

    private static final Logger log = LoggerFactory.getLogger(CacheHelper.class);

    private static final ExecutorService CACHE_REBUILD_ES = new ThreadPoolExecutor(
            2,
            2,
            30,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10),
            new SimpleNamedThreadFactory("CacheHelper-Rebuild"));

    public static final Long CACHE_NULL_TTL = 5 * 60 * 1000L;

    public static final String NULL_OBJECT = "NULL";

    public static final String PERFORMANCE_FIRST_PREFIX = "pff:";
    public static final String RT_DATA_FIRST_PREFIX = "rtf:";
    private static final String BUILD_SUCCESS_MSG = "构建缓存 [{}] 成功，获取到数据 [{}]。";
    private static final String UNEXPIRED_MSG = "获取到 [{}] 的数据 [{}] 未过期。";
    private static final String EXPIRED_MSG = "获取到 [{}] 的数据 [{}] 已过期。";
    private static final String CLEAR_MSG = "清理缓存 [{}] 成功。";

    private static final Class<?> TYPE_REFERENCE_CLASS = TypeReference.class;

    private static final long WAIT_MILL_SECONDS = 50L;

    private static final int LOOP = 3;

    private final KVCache<String, String> kvCache;

    private final LockService lockService;

    /**
     * 性能优先模式下过期检测间隔（ms）
     */
    private final long ppfExpirationDetectionInterval;

    private final Map<String, TrafficLimiter> trafficLimiterMap = new ConcurrentHashMap<>();

    public CacheHelper(KVCache<String, String> kvCache, LockService lockService, long ppfExpirationDetectionInterval) {
        this.kvCache = kvCache;
        this.lockService = lockService;
        this.ppfExpirationDetectionInterval = ppfExpirationDetectionInterval;
    }

    public CacheHelper(KVCache<String, String> kvCache, LockService lockService) {
        this.kvCache = kvCache;
        this.lockService = lockService;
        this.ppfExpirationDetectionInterval = 100L;
    }

    public <I> void acceptWithPerformanceFirst(String keyPrefix, I id, Consumer<I> consumer) {
        String key = keyPrefix + PERFORMANCE_FIRST_PREFIX + id;
        consumer.accept(id);
        kvCache.delete(key);
        log.info(CLEAR_MSG, key);
    }

    public <I> void acceptWithPerformanceFirstAsync(String keyPrefix, I id, Consumer<I> consumer) {
        String key = keyPrefix + PERFORMANCE_FIRST_PREFIX + id;
        consumer.accept(id);
        CACHE_REBUILD_ES.execute(() -> {
            kvCache.delete(key);
            log.info(CLEAR_MSG, key);
        });
    }

    public <I, R> R applyWithPerformanceFirst(String keyPrefix, I id, Function<I, R> function) {
        String key = keyPrefix + PERFORMANCE_FIRST_PREFIX + id;
        R apply = function.apply(id);
        kvCache.delete(key);
        log.info(CLEAR_MSG, key);
        return apply;
    }

    public <I, R> R applyWithPerformanceFirstAsync(String keyPrefix, I id, Function<I, R> function) {
        String key = keyPrefix + PERFORMANCE_FIRST_PREFIX + id;
        R apply = function.apply(id);
        CACHE_REBUILD_ES.execute(() -> {
            kvCache.delete(key);
            log.info(CLEAR_MSG, key);
        });
        return apply;
    }

    /**
     * 性能优先的缓存查询方法，基于逻辑过期实现。
     *
     * @param keyPrefix 与id一起构成完整的键
     * @param id        键值
     * @param rType     返回值类型
     * @param rTQuery   查询实时数据
     * @param ttl       过期时间
     * @param <R>       返回值类型
     * @param <I>       键值类型
     */
    public <R, I> R queryWithPerformanceFirst(
            String keyPrefix, I id, Class<R> rType, Function<I, R> rTQuery, Long ttl) {
        TypeReference<R> typeReference = new TypeReference<R>() {
            @Override
            public Type getType() {
                return rType;
            }
        };
        return queryWithPerformanceFirst(keyPrefix, id, typeReference, rTQuery, ttl);
    }


    /**
     * 性能优先的缓存查询方法，基于逻辑过期实现。
     *
     * @param keyPrefix 与id一起构成完整的键
     * @param id        键值
     * @param rType     返回值类型
     * @param rTQuery   查询实时数据
     * @param ttl       过期时间
     * @param <R>       返回值类型
     * @param <I>       键值类型
     */
    public <R, I> R queryWithPerformanceFirst(
            String keyPrefix, I id, TypeReference<R> rType, Function<I, R> rTQuery, Long ttl) {
        String key = keyPrefix + PERFORMANCE_FIRST_PREFIX + id;
        // 1 查询缓存
        String cachedJson = kvCache.get(key);
        // 2.1 缓存不存在则基于互斥锁构建缓存
        if (cachedJson == null) {
            // 查询数据库
            R r = queryWithRTDataFirst(keyPrefix, id, rType, rTQuery, ttl, false);
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
            // 4.1 判断是否过期，未过期，直接返回店铺信息
            if (expireTime.isAfter(LocalDateTime.now())) {
                log.debug(UNEXPIRED_MSG, key, data);
            }
            // 4.2 判断是否过期，已过期，需要缓存重建
            else {
                long threadId = UUID.randomUUID().toString().hashCode();
                // 5.1 获取互斥锁，成功，开启独立线程，进行缓存重建
                if (lockService.tryLock(getLockKey(key), threadId)) {
                    CACHE_REBUILD_ES.submit(() -> {
                        try {
                            // 查询数据库
                            R r = rTQuery.apply(id);
                            // 存在缓存里
                            setCacheData(key, r, ttl);
                        } catch (Exception e) {
                            log.error("构建缓存 [{}] 失败！", key, e);
                        } finally {
                            // 释放锁
                            lockService.unlock(getLockKey(key), threadId);
                        }
                    });
                    log.info("获取锁 [{}] 成功，提交了缓存重建任务，返回过期数据 [{}]。", getLockKey(key), data);
                }
                // 5.2 获取互斥锁，未成功不进行缓存重建
                else {
                    log.info("获取锁 [{}] 失败，未提交缓存重建任务，返回过期数据 [{}]。", getLockKey(key), data);
                }
                // 如果过期了，输出告警信息。
                // 使用限流器防止高并发下大量打印日志。
                TrafficLimiter trafficLimiter = trafficLimiterMap.computeIfAbsent(
                        keyPrefix + PERFORMANCE_FIRST_PREFIX, s -> new RequestIntervalTrafficLimiter(ppfExpirationDetectionInterval));
                if (trafficLimiter.acquire()) {
                    log.warn(EXPIRED_MSG, key, data);
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
        log.info(BUILD_SUCCESS_MSG, key, r);
    }

    public <I> void acceptWithRTDataFirst(String keyPrefix, I id, Consumer<I> consumer) {
        String key = keyPrefix + RT_DATA_FIRST_PREFIX + id;
        consumer.accept(id);
        kvCache.delete(key);
        log.info(CLEAR_MSG, key);
    }

    public <I> void acceptWithRTDataFirstAsync(String keyPrefix, I id, Consumer<I> consumer) {
        String key = keyPrefix + RT_DATA_FIRST_PREFIX + id;
        consumer.accept(id);
        CACHE_REBUILD_ES.execute(() -> {
            kvCache.delete(key);
            log.info(CLEAR_MSG, key);
        });
    }

    public <I, R> R applyWithRTDataFirst(String keyPrefix, I id, Function<I, R> function) {
        String key = keyPrefix + RT_DATA_FIRST_PREFIX + id;
        R apply = function.apply(id);
        kvCache.delete(key);
        log.info(CLEAR_MSG, key);
        return apply;
    }

    public <I, R> R applyWithRTDataFirstAsync(String keyPrefix, I id, Function<I, R> function) {
        String key = keyPrefix + RT_DATA_FIRST_PREFIX + id;
        R apply = function.apply(id);
        CACHE_REBUILD_ES.execute(() -> {
            kvCache.delete(key);
            log.info(CLEAR_MSG, key);
        });
        return apply;
    }

    /**
     * 实时数据优先的缓存查询方法，基于互斥锁实现。
     *
     * @param keyPrefix 与id一起构成完整的键
     * @param id        键值
     * @param rType     返回值类型
     * @param rTQuery   查询实时数据
     * @param ttl       过期时间
     * @param <R>       返回值类型
     * @param <I>       键值类型
     */
    public <R, I> R queryWithRTDataFirst(
            String keyPrefix, I id, TypeReference<R> rType, Function<I, R> rTQuery, Long ttl) {
        return queryWithRTDataFirst(keyPrefix, id, rType, rTQuery, ttl, true);
    }

    /**
     * 实时数据优先的缓存查询方法，基于互持锁实现。
     *
     * @param keyPrefix 与id一起构成完整的键
     * @param id        键值
     * @param rType     返回值类型
     * @param rTQuery   查询实时数据
     * @param ttl       过期时间
     * @param <R>       返回值类型
     * @param <I>       键值类型
     */
    public <R, I> R queryWithRTDataFirst(
            String keyPrefix, I id, Class<R> rType, Function<I, R> rTQuery, Long ttl) {
        TypeReference<R> typeReference = new TypeReference<R>() {
            @Override
            public Type getType() {
                return rType;
            }
        };
        return queryWithRTDataFirst(keyPrefix, id, typeReference, rTQuery, ttl, true);
    }

    /**
     * 实时数据优先的缓存查询方法，基于互持锁实现。
     *
     * @param keyPrefix 与id一起构成完整的键
     * @param id        键值
     * @param rType     返回值类型
     * @param rTQuery   查询实时数据
     * @param ttl       过期时间
     * @param cache     是否将实时查询的数据缓存
     * @param <R>       返回值类型
     * @param <I>       键值类型
     */
    public <R, I> R queryWithRTDataFirst(
            String keyPrefix, I id, TypeReference<R> rType, Function<I, R> rTQuery, Long ttl, boolean cache) {
        return doQueryWithRTDataFirst(keyPrefix, id, rType, rTQuery, ttl, 0, cache);
    }

    /**
     * 实时数据优先的缓存查询方法，基于互持锁实现。
     *
     * @param keyPrefix 与id一起构成完整的键
     * @param id        键值
     * @param rType     返回值类型
     * @param rTQuery   查询实时数据
     * @param ttl       过期时间
     * @param cache     是否将实时查询的数据缓存
     * @param <R>       返回值类型
     * @param <I>       键值类型
     */
    public <R, I> R queryWithRTDataFirst(
            String keyPrefix, I id, Class<R> rType, Function<I, R> rTQuery, Long ttl, boolean cache) {
        TypeReference<R> typeReference = new TypeReference<R>() {
            @Override
            public Type getType() {
                return rType;
            }
        };
        return doQueryWithRTDataFirst(keyPrefix, id, typeReference, rTQuery, ttl, 0, cache);
    }

    /**
     * 实时数据优先的缓存查询方法，基于互持锁实现。
     *
     * @param keyPrefix 与id一起构成完整的键
     * @param id        键值
     * @param rType     返回值类型
     * @param rTQuery   查询实时数据
     * @param ttl       过期时间
     * @param itr       第几次尝试
     * @param cache     是否将实时查询的数据缓存
     * @param <R>       返回值类型
     * @param <I>       键值类型
     */
    private <R, I> R doQueryWithRTDataFirst(
            String keyPrefix, I id, TypeReference<R> rType, Function<I, R> rTQuery, Long ttl, int itr, boolean cache) {
        String key = keyPrefix + RT_DATA_FIRST_PREFIX + id;
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
            // 4.1获取互斥锁，获取不到就休眠直至抛出异常
            if (!lockService.tryLock(getLockKey(key))) {
                try {
                    itr++;
                    if (itr == LOOP) {
                        log.error("第 [{}] 次未获取到锁 [{}]，终止获取锁！", itr, getLockKey(key));
                        throw new CacheException("系统繁忙，请稍后再试！", CacheException.BUSY);
                    } else {
                        log.info("第 [{}] 次未获取到锁 [{}]，休眠 [{}] ms", itr, getLockKey(key), WAIT_MILL_SECONDS);
                        Thread.sleep(WAIT_MILL_SECONDS);
                    }
                    return doQueryWithRTDataFirst(keyPrefix, id, rType, rTQuery, ttl, itr, cache);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("未获取到锁 [{}] 陷入休眠后被唤醒。", getLockKey(key));
                    throw new CacheException("系统繁忙，请稍后再试！", CacheException.BUSY);
                }
            }
            // 4.2获取互斥锁，获取到进行缓存构建
            else {
                try {
                    R r = rTQuery.apply(id);
                    if (cache) {
                        if (r == null) {
                            kvCache.set(key, NULL_OBJECT, Math.min(CACHE_NULL_TTL, ttl), TimeUnit.MILLISECONDS);
                        } else {
                            kvCache.set(key, BEJsonUtils.objectToString(r), ttl, TimeUnit.MILLISECONDS);
                        }
                        log.info(BUILD_SUCCESS_MSG, key, r);
                    }
                    return r;
                } finally {
                    lockService.unlock(getLockKey(key));
                }
            }
        }
    }

    private String getLockKey(String key) {
        return key + ":lock";
    }

    private <R> TypeReference<?> createRdTypeReference(TypeReference<R> typeReference) {
        TypeReference<Object> reference = new TypeReference<Object>() {
        };
        try {
            ParameterizedType a = ParameterizedTypeImpl.make(CacheData.class, new Type[]{typeReference.getType()}, null);
            Field typeField = TYPE_REFERENCE_CLASS.getDeclaredField("_type");
            typeField.setAccessible(true);
            typeField.set(reference, a);
            return reference;
        } catch (IllegalAccessException | NoSuchFieldException e) {
            log.error("无法设置 TypeReference 的 _type 属性！", e);
            throw new CacheException("无法设置 TypeReference 的 _type 属性！", CacheException.ERROR);
        }
    }

    public static class CacheData<T> {
        @JsonSerialize(using = LocalDateTimeStrSerializer.class)
        @JsonDeserialize(using = LocalDateTimeStrDeSerializer.class)
        private LocalDateTime expireTime;
        private T data;

        public CacheData() {
        }

        public CacheData(LocalDateTime expireTime, T data) {
            this.expireTime = expireTime;
            this.data = data;
        }

        public CacheData(T data) {
            this.data = data;
        }

        public LocalDateTime getExpireTime() {
            return expireTime;
        }

        public void setExpireTime(LocalDateTime expireTime) {
            this.expireTime = expireTime;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return "CacheData{" +
                    "expireTime=" + expireTime +
                    ", data=" + data +
                    '}';
        }
    }

}
