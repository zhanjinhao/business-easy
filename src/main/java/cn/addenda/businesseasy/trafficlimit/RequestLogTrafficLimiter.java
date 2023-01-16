package cn.addenda.businesseasy.trafficlimit;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 请求日志限流：限制总时间窗口内请求的总数量 & 同时记录所有请求的时间点
 *
 * @author addenda
 * @datetime 2022/12/28 14:15
 */
public class RequestLogTrafficLimiter implements TrafficLimiter {
    /**
     * 日志的记录器
     */
    private final Deque<Long> requestLogger = new ArrayDeque<>();
    /**
     * 计数阈值
     */
    private final long permits;
    /**
     * 日志的总时长（ms）
     */
    private final long duration;

    public RequestLogTrafficLimiter(long permits, long duration) {
        this.permits = permits;
        this.duration = duration;
    }

    @Override
    public synchronized long tryAcquire() {
        long now = System.currentTimeMillis();
        clearExpiredLog(now);
        // 如果请求的数量没到阈值，成功
        if (requestLogger.size() < permits) {
            requestLogger.addLast(now);
            return 0;
        }

        return -1;
    }

    /**
     * 清理所有的过期节点
     */
    private void clearExpiredLog(long now) {
        Long first;
        while ((first = requestLogger.getFirst()) != null) {
            if (now - first > duration) {
                requestLogger.removeFirst();
            } else {
                break;
            }
        }
    }

    @Override
    public boolean acquire() {
        return tryAcquire() == 0;
    }

    @Override
    public String toString() {
        return "RequestLogTrafficLimiter{" +
                ", permits=" + permits +
                ", duration=" + duration +
                '}';
    }
}
