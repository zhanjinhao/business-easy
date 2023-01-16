package cn.addenda.businesseasy.trafficlimit;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 请求间隔限流：限制请求的时间间隔 & 没有桶存放请求
 *
 * @author addenda
 * @datetime 2022/12/28 16:57
 */
@Slf4j
public class RequestIntervalTrafficLimiter implements TrafficLimiter {

    /**
     * 每秒允许通过的请求数量
     */
    private final double permitsPerSecond;
    /**
     * 每两次请求之间的间隔（ms）
     */
    private final long interval;
    /**
     * 上次请求通过的时间
     */
    private final AtomicLong latestPassedTime = new AtomicLong(-1L);

    public RequestIntervalTrafficLimiter(double permitsPerSecond) {
        this.interval = Math.round(1000 / permitsPerSecond);
        this.permitsPerSecond = permitsPerSecond;
        attributeCheck();
    }

    public RequestIntervalTrafficLimiter(long interval) {
        this.interval = interval;
        this.permitsPerSecond = 1000d / interval;
        attributeCheck();
    }

    private void attributeCheck() {
        if (interval * permitsPerSecond != 1000) {
            log.warn("interval * permitsPerSecond != 1000, interval: {}, permitsSecond: {}. ", interval, permitsPerSecond);
        }
    }

    /**
     * 间隔指定的时间才能获取到
     */
    @Override
    public long tryAcquire() {
        return acquire() ? 0 : -1;
    }

    @Override
    public boolean acquire() {
        long now = System.currentTimeMillis();
        long latest = latestPassedTime.get();
        if (now - latest > interval) {
            return latestPassedTime.compareAndSet(latest, now);
        }

        return false;
    }

    @Override
    public String toString() {
        return "RequestIntervalTrafficLimiter{" +
                "permitsPerSecond=" + permitsPerSecond +
                ", interval=" + interval +
                '}';
    }
}
