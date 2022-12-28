package cn.addenda.businesseasy.trafficlimit;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * @author addenda
 * @datetime 2022/12/28 19:20
 */
public class SlidingWindowTrafficLimiter implements TrafficLimiter {

    /**
     * 窗口内的计数阈值
     */
    private final long threshold;
    /**
     * 窗口的总时长（ms）
     */
    private final long duration;
    /**
     * 单个窗口的时长（ms）
     */
    private final long sliceDuration;
    /**
     * 计数器： k-前窗口的开始时间（ms），value-当前窗口的计数
     */
    private final TreeMap<Long, Integer> counters;

    public SlidingWindowTrafficLimiter(long threshold, long duration, long sliceDuration) {
        this.threshold = threshold;
        this.duration = duration;
        this.sliceDuration = sliceDuration;
        this.counters = new TreeMap<>();
    }

    @Override
    public synchronized boolean tryAcquire() {
        // 获取当前时间所在的子窗口值
        long currentWindowTime = System.currentTimeMillis() / sliceDuration * sliceDuration;
        // 获取当前窗口的请求总量
        int currentWindowCount = getCurrentWindowCount(currentWindowTime);
        if (currentWindowCount >= threshold) {
            return false;
        }

        // 计数器 + 1
        counters.merge(currentWindowTime, 1, Integer::sum);

        return true;
    }


    /**
     * 获取当前窗口中的所有请求数（并删除所有无效的子窗口计数器）
     *
     * @param currentWindowTime 当前子窗口时间
     * @return 当前窗口中的计数
     */
    private int getCurrentWindowCount(long currentWindowTime) {
        // 计算出窗口的开始位置时间
        long startTime = currentWindowTime - duration + sliceDuration;
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
    public String toString() {
        return "SlidingWindowRateLimiter{" +
            "threshold=" + threshold +
            ", duration=" + duration +
            ", sliceDuration=" + sliceDuration +
            '}';
    }

}


