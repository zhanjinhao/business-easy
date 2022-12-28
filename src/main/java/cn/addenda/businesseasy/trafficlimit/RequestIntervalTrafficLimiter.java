package cn.addenda.businesseasy.trafficlimit;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author addenda
 * @datetime 2022/12/28 16:57
 */
public class RequestIntervalTrafficLimiter implements TrafficLimiter {

    private final long interval;

    private final AtomicLong latestPassedTime = new AtomicLong(-1L);

    public RequestIntervalTrafficLimiter(long interval) {
        this.interval = interval;
    }

    /**
     * 间隔指定的时间才能获取到
     */
    @Override
    public boolean tryAcquire() {
        long now = System.currentTimeMillis();
        long lasted = latestPassedTime.get();
        if (now - lasted > interval) {
            return latestPassedTime.compareAndSet(lasted, now);
        }

        return false;
    }

    @Override
    public String toString() {
        return "RequestIntervalTrafficLimiter{" +
            "interval=" + interval +
            ", latestPassedTime=" + latestPassedTime +
            '}';
    }
}
