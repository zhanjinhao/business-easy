package cn.addenda.businesseasy.trafficlimit;

import cn.addenda.businesseasy.util.BESleepUtils;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * 漏桶限流：限制请求的时间间隔 & 时间间隔不到且等待的请求不超过桶容量时在桶里等待
 *
 * @author addenda
 * @datetime 2022/12/28 14:15
 */
@Slf4j
public class LeakyBucketTrafficLimiter implements TrafficLimiter {

    /**
     * 最大等待时间（ms）
     */
    private final long maxQueueingTime;
    /**
     * 每秒允许通过的请求数量
     */
    private final double permitsPerSecond;
    /**
     * 每两次请求之间的间隔（ms）
     */
    private final long interval;
    /**
     * 桶的总容量
     */
    private final long capacity;
    /**
     * 上一次成功的时间
     */
    private long latestPassedTime = -1;

    public LeakyBucketTrafficLimiter(long maxQueueingTime, double permitsPerSecond) {
        this.maxQueueingTime = maxQueueingTime;
        this.permitsPerSecond = permitsPerSecond;
        this.interval = Math.round(1000 / permitsPerSecond);
        this.capacity = Math.round(maxQueueingTime * permitsPerSecond / 1000);
        attributeCheck();
    }

    private void attributeCheck() {
        if (interval * permitsPerSecond != 1000) {
            log.warn("interval * permitsPerSecond != 1000, interval: {}, permitsSecond: {}. ", interval, permitsPerSecond);
        }
        if (capacity * 1000 != maxQueueingTime * permitsPerSecond) {
            log.warn("capacity * 1000 != maxQueueingTime * permitsPerSecond, capacity: {}, maxQueueingTime: {}, permitsPerSecond: {}. ",
                    capacity, maxQueueingTime, permitsPerSecond);
        }
    }

    @Override
    public synchronized long tryAcquire() {
        long now = System.currentTimeMillis();
        long expectedTime = interval + latestPassedTime;

        if (expectedTime <= now) {
            latestPassedTime = now;
            return 0;
        } else {
            long waitTime = expectedTime - now;
            if (waitTime > maxQueueingTime) {
                return -1;
            }
            latestPassedTime = expectedTime;
            return waitTime;
        }
    }

    /**
     * 能通过的请求是匀速的。
     */
    @Override
    public boolean acquire() {
        long waitTime = tryAcquire();
        if (waitTime > 0) {
            BESleepUtils.sleep(TimeUnit.MILLISECONDS, waitTime);
            return true;
        }
        return waitTime == 0;
    }

    @Override
    public String toString() {
        return "LeakyBucketTrafficLimiter{" +
                "maxQueueingTime=" + maxQueueingTime +
                ", permitsPerSecond=" + permitsPerSecond +
                ", interval=" + interval +
                ", capacity=" + capacity +
                '}';
    }
}
