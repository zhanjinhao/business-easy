package cn.addenda.businesseasy.trafficlimit;

import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * 滑动窗口限流：限制总时间窗口内请求的总数量。
 *
 * @author addenda
 * @datetime 2022/12/28 19:20
 */
@Slf4j
public class SlidingWindowTrafficLimiter implements TrafficLimiter {

    /**
     * 总的计数阈值
     */
    private final long permits;
    /**
     * 窗口的总时长（ms）
     */
    private final long duration;
    /**
     * 单个窗口的时长（ms）
     */
    private final long windowDuration;
    /**
     * 窗口的个数
     */
    private final int windowCount;
    /**
     * 是否是乐观模式：<p/>
     * 1.乐观模式可能会造成流量超出预期，但吞吐量大<p/>
     * 2.非乐观模式保证流量一定不会超出预期，但吞吐量小
     */
    private final boolean optimistic;
    /**
     * 计数器： k-前窗口的开始时间（ms），value-当前窗口的计数
     */
    private final TreeMap<Long, Integer> counters;

    public SlidingWindowTrafficLimiter(long permits, long duration, long windowDuration, boolean optimistic) {
        this.permits = permits;
        this.duration = duration;
        this.windowDuration = windowDuration;
        this.windowCount = (int) (duration / windowDuration);
        this.optimistic = optimistic;
        this.counters = new TreeMap<>();
        attributeCheck();
    }

    public SlidingWindowTrafficLimiter(long permits, long duration, int windowCount, boolean optimistic) {
        this.permits = permits;
        this.duration = duration;
        this.windowCount = windowCount;
        this.windowDuration = duration / windowCount;
        this.optimistic = optimistic;
        this.counters = new TreeMap<>();
        attributeCheck();
    }

    private void attributeCheck() {
        if (windowCount * windowDuration != duration) {
            log.warn("windowCount * windowDuration != duration, windowCount: {}, windowDuration: {}, duration: {}.",
                    windowCount, windowDuration, duration);
        }
    }

    @Override
    public synchronized long tryAcquire() {
        // 获取当前时间所在的子窗口值
        long currentWindowTime = System.currentTimeMillis() / windowDuration * windowDuration;
        // 获取当前窗口的请求总量
        int currentWindowCount = getCurrentWindowCount(currentWindowTime);
        if (currentWindowCount >= permits) {
            return -1;
        }

        // 计数器 + 1
        counters.merge(currentWindowTime, 1, Integer::sum);

        return 0;
    }

    /**
     * 获取当前窗口中的所有请求数（并删除所有无效的子窗口计数器）
     *
     * @param currentWindowTime 当前子窗口时间
     * @return 当前窗口中的计数
     */
    private int getCurrentWindowCount(long currentWindowTime) {
        // 计算出窗口的开始位置时间
        long startTime = currentWindowTime - duration + (optimistic ? windowDuration : 0);
        int result = 0;

        // 遍历当前存储的计数器，删除无效的子窗口计数器，并累加当前窗口中的所有计数器之和
        Iterator<Entry<Long, Integer>> iterator = counters.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<Long, Integer> entry = iterator.next();
            if (entry.getKey() < startTime) {
                iterator.remove();
            } else {
                result += entry.getValue();
            }
        }

        return result;
    }

    @Override
    public boolean acquire() {
        return tryAcquire() == 0;
    }

    @Override
    public String toString() {
        return "SlidingWindowTrafficLimiter{" +
                "permits=" + permits +
                ", duration=" + duration +
                ", windowDuration=" + windowDuration +
                ", windowCount=" + windowCount +
                ", optimistic=" + optimistic +
                '}';
    }
}
