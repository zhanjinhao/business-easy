package cn.addenda.businesseasy.trafficlimit;

import java.util.concurrent.atomic.AtomicLong;

/**
 * copy from sentinel project
 *
 * @author addenda
 * @datetime 2022/12/28 14:15
 */
public class LeakyBucketTrafficLimiter implements TrafficLimiter {

    /**
     * 最大等待时间（ms）
     */
    private final int maxQueueingTime;
    /**
     * 每秒允许通过的请求数量
     */
    private final double qps;
    /**
     * 上一次成功的时间
     */
    private final AtomicLong latestPassedTime = new AtomicLong(-1);

    public LeakyBucketTrafficLimiter(int maxQueueingTime, double qps) {
        this.maxQueueingTime = maxQueueingTime;
        this.qps = qps;
    }

    /**
     * 能通过的请求是匀速的。
     */
    @Override
    public boolean tryAcquire() {
        // Reject when count is less or equal than 0.
        // Otherwise,the costTime will be max of long and waitTime will overflow in some cases.
        if (qps <= 0) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        // Calculate the interval between every two requests.
        long costTime = Math.round(1000 / qps);

        // Expected pass time of this request.
        long expectedTime = costTime + latestPassedTime.get();

        // 当前时间 > 期望时间
        if (expectedTime <= currentTime) {
            // Contention may exist here, but it's okay.
            return latestPassedTime.compareAndSet(latestPassedTime.get(), currentTime);
        } else {
            // Calculate the time to wait.
            // 不使用expectedTime，是为了重新获取一遍latestPassedTime，有可能别的线程修改值了
            long waitTime = costTime + latestPassedTime.get() - System.currentTimeMillis();
            // 等待时间 > 最大排队时间
            if (waitTime > maxQueueingTime) {
                return false;
            }

            // 上次时间 + 间隔时间
            expectedTime = latestPassedTime.addAndGet(costTime);
            // 等待时间
            waitTime = expectedTime - System.currentTimeMillis();
            // 等待时间 > 最大排队时间
            if (waitTime > maxQueueingTime) {
                latestPassedTime.addAndGet(-costTime);
                return false;
            }

            // in race condition waitTime may <= 0
            if (waitTime > 0) {
                sleepMs(waitTime);
            }
            // 等待完了，就放行
            return true;
        }
    }

    private void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public String toString() {
        return "LeakyBucketTrafficLimiter{" +
            "maxQueueingTime=" + maxQueueingTime +
            ", qps=" + qps +
            ", latestPassedTime=" + latestPassedTime +
            '}';
    }
}
