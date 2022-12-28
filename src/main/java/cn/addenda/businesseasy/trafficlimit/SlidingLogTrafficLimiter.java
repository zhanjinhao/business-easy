package cn.addenda.businesseasy.trafficlimit;

/**
 * @author addenda
 * @datetime 2022/12/28 14:02
 */
public class SlidingLogTrafficLimiter implements TrafficLimiter {

    private final SlidingWindowTrafficLimiter slidingWindowTrafficLimiter;

    /**
     * 窗口内的计数阈值
     */
    private final long threshold;
    /**
     * 日志
     */
    private final long duration;

    public SlidingLogTrafficLimiter(long duration, long threshold) {
        this.duration = duration;
        this.threshold = threshold;
        slidingWindowTrafficLimiter = new SlidingWindowTrafficLimiter(threshold, duration, 1);
    }

    @Override
    public boolean tryAcquire() {
        return slidingWindowTrafficLimiter.tryAcquire();
    }

    @Override
    public String toString() {
        return "SlidingLogRateLimiter{" +
            "threshold=" + threshold +
            ", duration=" + duration +
            '}';
    }
}
