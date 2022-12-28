package cn.addenda.businesseasy.trafficlimit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * @author addenda
 * @datetime 2022/12/28 14:15
 */
public class RequestLogTrafficLimiter implements TrafficLimiter {

    private final Deque<Long> requestLogger = new ArrayDeque<>();
    /**
     * 窗口内的计数阈值
     */
    private final long threshold;
    /**
     * 日志的总时长（ms）
     */
    private final long duration;

    public RequestLogTrafficLimiter(long threshold, long duration) {
        this.threshold = threshold;
        this.duration = duration;
    }

    @Override
    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        // 清理所有的过期节点
        Iterator<Long> iterator = requestLogger.iterator();
        while (iterator.hasNext()) {
            Long next = iterator.next();
            if (now - next > duration) {
                iterator.remove();
            } else {
                break;
            }
        }

        // 如果请求的数量没到阈值，成功
        if (requestLogger.size() < threshold) {
            requestLogger.addLast(now);
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return "RequestLogTrafficLimiter{" +
            "threshold=" + threshold +
            ", duration=" + duration +
            '}';
    }
}
