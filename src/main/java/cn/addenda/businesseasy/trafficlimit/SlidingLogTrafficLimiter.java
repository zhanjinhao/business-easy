package cn.addenda.businesseasy.trafficlimit;

import lombok.extern.slf4j.Slf4j;

/**
 * 滑动日志限流：
 * <p>
 * 统计每毫秒请求的数量并以此限制总时间窗口内请求的总数量，
 * <p>
 * 当前时间距离本毫秒开始时间不足一毫秒按一毫秒计算。
 *
 * @author addenda
 * @datetime 2022/12/28 14:02
 */
@Slf4j
public class SlidingLogTrafficLimiter implements TrafficLimiter {

    private final SlidingWindowTrafficLimiter slidingWindowTrafficLimiter;

    /**
     * 总的计数阈值
     */
    private final long permits;
    /**
     * 日志的总时长（ms）
     */
    private final long duration;

    public SlidingLogTrafficLimiter(long permits, long duration) {
        this.permits = permits;
        this.duration = duration;
        slidingWindowTrafficLimiter = new SlidingWindowTrafficLimiter(permits, duration, 1L, true);
    }

    @Override
    public long tryAcquire() {
        return slidingWindowTrafficLimiter.tryAcquire();
    }

    @Override
    public boolean acquire() {
        return slidingWindowTrafficLimiter.acquire();
    }

    @Override
    public String toString() {
        return "SlidingLogTrafficLimiter{" +
                "permits=" + permits +
                ", duration=" + duration +
                '}';
    }
}
